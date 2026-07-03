package com.jipdaum_spring.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secret",
                "test-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm");
        ReflectionTestUtils.setField(jwtTokenProvider, "expiration", 3600000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpiration", 1209600000L);
    }

    @Test
    void 발급한_access_token에서_이메일을_그대로_꺼낼_수_있다() {
        String token = jwtTokenProvider.generateAccessToken("user@example.com");

        assertThat(jwtTokenProvider.getEmail(token)).isEqualTo("user@example.com");
        assertThat(jwtTokenProvider.validateToken(token)).isEqualTo(JwtTokenProvider.TokenStatus.VALID);
    }

    @Test
    void 만료된_토큰은_EXPIRED로_판정된다() {
        ReflectionTestUtils.setField(jwtTokenProvider, "expiration", -1000L);
        String expiredToken = jwtTokenProvider.generateAccessToken("user@example.com");

        assertThat(jwtTokenProvider.validateToken(expiredToken)).isEqualTo(JwtTokenProvider.TokenStatus.EXPIRED);
        assertThat(jwtTokenProvider.validate(expiredToken)).isFalse();
    }

    @Test
    void 서명이_다른_토큰은_INVALID로_판정된다() {
        String token = jwtTokenProvider.generateAccessToken("user@example.com");

        JwtTokenProvider otherSigner = new JwtTokenProvider();
        ReflectionTestUtils.setField(otherSigner, "secret",
                "a-completely-different-jwt-secret-key-with-enough-length-for-hs256");
        ReflectionTestUtils.setField(otherSigner, "expiration", 3600000L);
        ReflectionTestUtils.setField(otherSigner, "refreshExpiration", 1209600000L);

        assertThat(otherSigner.validateToken(token)).isEqualTo(JwtTokenProvider.TokenStatus.INVALID);
    }

    @Test
    void user_id_클레임만_있는_토큰은_getUserId로_읽을_수_있다() {
        // Django SimpleJWT 발급 토큰 호환: sub 없이 정수 user_id 클레임만 있는 경우를 흉내낸다.
        String token = io.jsonwebtoken.Jwts.builder()
                .claim("user_id", 42)
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        "test-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm"
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtTokenProvider.getEmail(token)).isNull();
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(42L);
    }

    @Test
    void access_token과_refresh_token은_type_클레임으로_구분된다() {
        String access = jwtTokenProvider.generateAccessToken("user@example.com");
        String refresh = jwtTokenProvider.generateRefreshToken("user@example.com");

        assertThat(jwtTokenProvider.getType(access)).isEqualTo(JwtTokenProvider.TYPE_ACCESS);
        assertThat(jwtTokenProvider.getType(refresh)).isEqualTo(JwtTokenProvider.TYPE_REFRESH);
    }

    @Test
    void type_클레임이_없는_레거시_토큰은_null을_반환한다() {
        // Django SimpleJWT 등 우리 쪽 "type" 클레임을 붙이지 않는 발급자를 흉내낸다.
        String token = io.jsonwebtoken.Jwts.builder()
                .claim("user_id", 1)
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        "test-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm"
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtTokenProvider.getType(token)).isNull();
    }

    @Test
    void 만료된_토큰이라도_type_클레임은_읽을_수_있다() {
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpiration", -1000L);
        String expiredRefresh = jwtTokenProvider.generateRefreshToken("user@example.com");

        assertThat(jwtTokenProvider.getType(expiredRefresh)).isEqualTo(JwtTokenProvider.TYPE_REFRESH);
    }
}
