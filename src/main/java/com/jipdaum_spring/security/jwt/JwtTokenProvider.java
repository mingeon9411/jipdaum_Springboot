package com.jipdaum_spring.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String email) {
        return Jwts.builder()
                .subject(email)
                .claim("type", TYPE_ACCESS)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .subject(email)
                .claim("type", TYPE_REFRESH)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 토큰의 "type" 클레임(access/refresh)을 읽는다. 만료된 토큰이라도 타입은 알 수 있어야
     * "만료된 access token"과 "애초에 access token이 아님"을 구분할 수 있으므로, 만료 예외에서도
     * claims를 꺼내 읽는다. 서명이 잘못됐거나 파싱 자체가 불가능하면 null을 반환한다.
     */
    public String getType(String token) {
        try {
            return Jwts.parser().verifyWith(getSigningKey()).build()
                    .parseSignedClaims(token).getPayload().get("type", String.class);
        } catch (ExpiredJwtException e) {
            return e.getClaims().get("type", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    // 기존 호환성 유지
    public String generateToken(String email) {
        return generateAccessToken(email);
    }

    public String getEmail(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // Django SimpleJWT는 sub 대신 user_id 클레임(정수)을 사용
    public Long getUserId(String token) {
        Object userId = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("user_id");
        if (userId == null) return null;
        if (userId instanceof Integer) return ((Integer) userId).longValue();
        if (userId instanceof Long) return (Long) userId;
        try { return Long.parseLong(userId.toString()); } catch (NumberFormatException e) { return null; }
    }

    public boolean validate(String token) {
        return validateToken(token) == TokenStatus.VALID;
    }

    /** 만료(EXPIRED)와 그 외 무효(INVALID)를 구분해서 알려준다. */
    public TokenStatus validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return TokenStatus.VALID;
        } catch (ExpiredJwtException e) {
            return TokenStatus.EXPIRED;
        } catch (Exception e) {
            return TokenStatus.INVALID;
        }
    }

    public enum TokenStatus {
        VALID, EXPIRED, INVALID
    }
}