package com.jipdaum_spring.config;

import com.jipdaum_spring.security.jwt.JwtAuthenticationFilter;
import com.jipdaum_spring.security.oauth.CustomOAuth2UserService;
import com.jipdaum_spring.security.oauth.ForceReloginAuthorizationRequestResolver;
import com.jipdaum_spring.security.oauth.OAuth2FailureHandler;
import com.jipdaum_spring.security.oauth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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
    private final ForceReloginAuthorizationRequestResolver forceReloginAuthorizationRequestResolver;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl));
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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth2/**", "/login/**",
                                "/api/auth/refresh", "/api/auth/social-exchange",
                                "/api/users/register", "/api/users/login",
                                "/api/users/me", "/api/users/nickname-check").permitAll()
                        // 상품 조회와 챗봇은 비로그인 방문자도 이용 가능해야 하므로 공개 유지.
                        // 장바구니/주문/쿠폰/리뷰 작성 등 나머지 /api/shop/**는 로그인이 필요하다 —
                        // 서비스 계층의 CurrentUserProvider 체크에만 기대지 않고 필터 단계에서도 강제한다.
                        .requestMatchers(HttpMethod.GET, "/api/shop/products/**").permitAll()
                        .requestMatchers("/api/shop/chat").permitAll()
                        .requestMatchers("/api/shop/**").authenticated()
                        .requestMatchers("/api/users/logout", "/api/users/withdraw", "/api/users/email-verify/**").authenticated()
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
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}