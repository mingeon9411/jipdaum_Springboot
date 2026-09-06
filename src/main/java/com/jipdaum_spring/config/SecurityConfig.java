package com.jipdaum_spring.config;

import com.jipdaum_spring.security.RestAuthenticationEntryPoint;
import com.jipdaum_spring.security.jwt.JwtAuthenticationFilter;
import com.jipdaum_spring.security.oauth.CustomOAuth2UserService;
import com.jipdaum_spring.security.oauth.ForceReloginAuthorizationRequestResolver;
import com.jipdaum_spring.security.oauth.OAuth2FailureHandler;
import com.jipdaum_spring.security.oauth.OAuth2SuccessHandler;
import com.jipdaum_spring.security.oauth.SocialLoginCaptchaFilter;
import com.jipdaum_spring.security.ratelimit.ChatRateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ChatRateLimitFilter chatRateLimitFilter;
    private final SocialLoginCaptchaFilter socialLoginCaptchaFilter;
    private final ForceReloginAuthorizationRequestResolver forceReloginAuthorizationRequestResolver;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // OAuth2 리다이렉트(frontendUrl)는 값 하나로 고정이지만, CORS는 로컬 dev + 배포 등
    // 여러 origin을 동시에 허용해야 할 수 있어 콤마로 구분된 목록을 따로 받는다.
    // 기본값을 frontend-url로 잡아둔 건 필수가 아니라 하위 호환 때문 — EC2의
    // application.yml은 이 저장소 파일과 별개(gitignore)라 이 프로퍼티가 아직 없으면
    // ${app.cors-allowed-origins}를 못 찾아 컨텍스트 초기화가 실패해 배포가 통째로
    // 죽는다(2026-08-28 실제 발생: 502로 프로덕션 전체 API가 막힘). 값이 있으면 그걸 쓴다.
    @Value("#{'${app.cors-allowed-origins:${app.frontend-url}}'.split(',')}")
    private List<String> corsAllowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsAllowedOrigins.stream().map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                // oauth2Login()의 기본 진입점은 인증 실패 시 /login으로 302 리다이렉트한다.
                // 이 앱은 순수 REST API라 그건 axios가 그대로 따라가버려 크래시로 이어지므로,
                // /api/**만 골라서 항상 401 JSON을 받도록 강제한다. authenticationEntryPoint(...)로
                // 통째로 덮어쓰면 oauth2Login()이 내부적으로 등록하는 자기 진입점까지 같이
                // 사라지므로, defaultAuthenticationEntryPointFor + 매처로 /api/** 범위만 좁힌다.
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        restAuthenticationEntryPoint, new AntPathRequestMatcher("/api/**")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth2/**", "/login/**",
                                "/api/auth/refresh", "/api/auth/social-exchange", "/api/auth/social-captcha",
                                "/api/users/register", "/api/users/login",
                                "/api/users/me", "/api/users/nickname-check",
                                // 아이디/비밀번호 찾기 — 로그인 전 상태에서 호출되므로 공개 필요.
                                "/api/users/security-question", "/api/users/find-id/**",
                                "/api/users/find-password/**").permitAll()
                        // 업로드된 리뷰 사진 등은 정적 파일 서빙이라 비로그인 방문자도 볼 수 있어야 한다.
                        // (업로드 자체는 /api/shop/uploads/**로, 아래 /api/shop/** authenticated 규칙에 걸린다)
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        // 상품 조회와 챗봇은 비로그인 방문자도 이용 가능해야 하므로 공개 유지.
                        // 장바구니/주문/쿠폰/리뷰 작성 등 나머지 /api/shop/**는 로그인이 필요하다 —
                        // 서비스 계층의 CurrentUserProvider 체크에만 기대지 않고 필터 단계에서도 강제한다.
                        .requestMatchers(HttpMethod.GET, "/api/shop/products/**").permitAll()
                        .requestMatchers("/api/shop/chat").permitAll()
                        .requestMatchers("/api/shop/**").authenticated()
                        .requestMatchers("/api/users/logout", "/api/users/withdraw", "/api/users/email-verify/**",
                                "/api/users/security-qa").authenticated()
                        .requestMatchers("/api/admin/**", "/api/users/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestResolver(forceReloginAuthorizationRequestResolver)
                        )
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // JWT 필터 뒤에 붙여야 로그인 사용자를 계정 단위로 구분해 rate limit을 걸 수 있다.
                .addFilterAfter(chatRateLimitFilter, JwtAuthenticationFilter.class)
                // OAuth2AuthorizationRequestRedirectFilter가 /oauth2/authorization/{id}를 가로채
                // provider로 리다이렉트해버리므로, 그 전에 ticket을 검사해야 한다.
                .addFilterBefore(socialLoginCaptchaFilter, OAuth2AuthorizationRequestRedirectFilter.class);

        return http.build();
    }
}