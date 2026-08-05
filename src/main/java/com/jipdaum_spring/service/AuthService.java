package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.jipdaumuser.JipdaumUserRepository;
import com.jipdaum_spring.domain.token.BlacklistedTokenRepository;
import com.jipdaum_spring.dto.auth.AccessTokenResponse;
import com.jipdaum_spring.dto.auth.TokenPairResponse;
import com.jipdaum_spring.exception.AuthException;
import com.jipdaum_spring.security.jwt.JwtTokenProvider;
import com.jipdaum_spring.security.oauth.SocialLoginCodeStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * JWT 발급/재발급을 담당한다. 회원가입/로그인/로그아웃 등 계정 자체의 처리는 UserAuthService 참고.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final JipdaumUserRepository jipdaumUserRepository;
    private final SocialLoginCodeStore socialLoginCodeStore;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    /**
     * OAuth2 로그인 성공 후 리다이렉트로 전달받은 1회용 code를 실제 access/refresh 토큰으로 교환한다.
     * code는 60초간만 유효하고 조회 즉시 폐기되므로, 이 호출은 콜백당 1회만 성공한다.
     */
    public TokenPairResponse exchangeSocialCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "code가 필요합니다.");
        }

        String email = socialLoginCodeStore.consume(code);
        if (email == null) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 code입니다.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(email);
        String refreshToken = jwtTokenProvider.generateRefreshToken(email);
        return new TokenPairResponse(accessToken, refreshToken);
    }

    public AccessTokenResponse refresh(String refreshToken) {
        // access token으로 재발급을 시도하는 것을 막는다. type 클레임이 없는 레거시(Django) 토큰은
        // 계속 통과시켜 기존 호환성을 유지한다.
        if (!StringUtils.hasText(refreshToken)
                || JwtTokenProvider.TYPE_ACCESS.equals(jwtTokenProvider.getType(refreshToken))
                || !jwtTokenProvider.validate(refreshToken)
                || blacklistedTokenRepository.existsByToken(refreshToken)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }

        String email = jwtTokenProvider.getEmail(refreshToken);

        // Django refresh token: sub 없이 user_id 클레임 사용
        if (email == null) {
            Long userId = jwtTokenProvider.getUserId(refreshToken);
            if (userId != null) {
                email = jipdaumUserRepository.findById(userId)
                        .map(u -> u.getEmail())
                        .orElse(null);
            }
        }

        if (email == null) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Cannot identify user from token");
        }

        // Spring Boot 형식의 새 access token 발급 (sub = email)
        String newAccessToken = jwtTokenProvider.generateAccessToken(email);
        return new AccessTokenResponse(newAccessToken);
    }
}
