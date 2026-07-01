package com.jipdaum_spring.security.oauth;

import com.jipdaum_spring.domain.User.User;
import com.jipdaum_spring.domain.User.UserRepository;
import com.jipdaum_spring.domain.user.JipdaumUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final JipdaumUserRepository jipdaumUserRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @SuppressWarnings("unchecked")
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email;
        String name;
        String profileImage;
        String providerId;

        switch (registrationId) {
            case "google" -> {
                email = (String) attributes.get("email");
                name = (String) attributes.get("name");
                profileImage = (String) attributes.get("picture");
                providerId = (String) attributes.get("sub");
            }
            case "kakao" -> {
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                providerId = String.valueOf(attributes.get("id"));
                email = "kakao_" + providerId;
                name = (String) profile.get("nickname");
                profileImage = (String) profile.get("profile_image_url");
            }
            case "naver" -> {
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                email = (String) response.get("email");
                name = (String) response.get("name");
                profileImage = (String) response.get("profile_image");
                providerId = (String) response.get("id");
            }
            default -> throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        }

        final String finalEmail = email;
        final String finalName = name;
        final String finalProfileImage = profileImage;

        User user = userRepository.findByProviderAndProviderId(registrationId, providerId)
                .map(u -> u.update(finalName, finalProfileImage))
                .orElse(User.builder()
                        .email(email)
                        .name(name)
                        .profileImage(profileImage)
                        .provider(registrationId)
                        .providerId(providerId)
                        .role(User.Role.USER)
                        .build());
        userRepository.save(user);

        // JIPDAUM_USER(Django 공유 테이블)에도 사용자 존재 보장 — Order FK에 필요
        String jipdaumEmail = ensureJipdaumUser(email, registrationId + "_" + providerId, name != null ? name : "소셜사용자");

        return new CustomOAuth2User(oAuth2User, jipdaumEmail);
    }

    private String ensureJipdaumUser(String email, String username, String nickname) {
        if (jipdaumUserRepository.findByEmail(email).isPresent()) {
            return email;
        }
        try {
            jdbcTemplate.update(
                "INSERT INTO JIPDAUM_USER " +
                "(username, password, last_login, is_superuser, first_name, last_name, " +
                "email, is_staff, is_active, created_at, nickname, is_email_verified) " +
                "VALUES (?, ?, NULL, 0, '', '', ?, 0, 1, SYSDATE, ?, 0)",
                username, "!" + username, email, nickname
            );
        } catch (Exception e) {
            // username 충돌 시 밀리초 suffix로 재시도
            try {
                jdbcTemplate.update(
                    "INSERT INTO JIPDAUM_USER " +
                    "(username, password, last_login, is_superuser, first_name, last_name, " +
                    "email, is_staff, is_active, created_at, nickname, is_email_verified) " +
                    "VALUES (?, ?, NULL, 0, '', '', ?, 0, 1, SYSDATE, ?, 0)",
                    username + "_" + (System.currentTimeMillis() % 10000), "!" + username, email, nickname
                );
            } catch (Exception ignored) {}
        }
        return email;
    }
}