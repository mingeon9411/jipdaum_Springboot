package com.jipdaum_spring.security.oauth;

import com.jipdaum_spring.domain.springuser.User;
import com.jipdaum_spring.domain.springuser.UserRepository;
import com.jipdaum_spring.security.JipdaumUserProvisioner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final JipdaumUserProvisioner jipdaumUserProvisioner;

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
                if (kakaoAccount == null) {
                    throw new OAuth2AuthenticationException(
                            "카카오 응답에 kakao_account가 없습니다. 카카오 개발자센터에서 닉네임 동의항목이 켜져 있는지 확인하세요.");
                }
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile == null) {
                    throw new OAuth2AuthenticationException(
                            "카카오 응답에 profile이 없습니다. 카카오 개발자센터에서 닉네임 동의항목이 켜져 있는지 확인하세요.");
                }
                providerId = String.valueOf(attributes.get("id"));
                email = "kakao_" + providerId;
                name = (String) profile.get("nickname");
                profileImage = (String) profile.get("profile_image_url");
            }
            case "naver" -> {
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                if (response == null) {
                    throw new OAuth2AuthenticationException(
                            "네이버 응답에 response가 없습니다. 네이버 개발자센터에서 필수 제공 항목 설정을 확인하세요.");
                }
                name = (String) response.get("name");
                profileImage = (String) response.get("profile_image");
                providerId = (String) response.get("id");
                String naverEmail = (String) response.get("email");
                if (naverEmail == null) {
                    // 네이버 개발자센터에서 이메일 제공 항목이 꺼져 있으면 응답에 email이 없다.
                    // 카카오와 동일하게 로그인 자체는 막지 않고 provider 기반 placeholder를 쓴다.
                    log.warn("네이버 응답에 email이 없습니다. 네이버 개발자센터의 제공 정보(이메일) 설정을 확인하세요. (providerId={})", providerId);
                    email = "naver_" + providerId;
                } else {
                    email = naverEmail;
                }
            }
            default -> throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        }

        final String finalName = name;
        final String finalProfileImage = profileImage;

        // 이전에 이미 이 provider+providerId로 가입된 계정이 있으면 그때 확정된 email(또는
        // placeholder)을 계속 써야 한다. 특히 네이버는 이메일 제공 동의 여부가 로그인마다 달라질 수
        // 있는데, 여기서 새로 받아온 email로 덮어써 버리면 JIPDAUM_USER 조회가 어긋나면서 기존 주문/
        // 쿠폰 내역이 없는 별도 계정이 새로 생기고 마이페이지가 텅 비어 보이는 문제가 생긴다.
        User existingUser = userRepository.findByProviderAndProviderId(registrationId, providerId).orElse(null);
        if (existingUser != null && existingUser.getEmail() != null) {
            email = existingUser.getEmail();
        }
        final String finalEmail = email;

        // users 테이블은 프로필 이미지 등 부가 정보 보조 저장용이라, 조회/저장이 실패해도
        // 로그인 자체(JIPDAUM_USER 기준)는 막지 않는다.
        try {
            User user = existingUser != null
                    ? existingUser.update(finalName, finalProfileImage)
                    : User.builder()
                            .email(finalEmail)
                            .name(name)
                            .profileImage(profileImage)
                            .provider(registrationId)
                            .providerId(providerId)
                            .role(User.Role.USER)
                            .build();
            userRepository.save(user);
        } catch (DataAccessException e) {
            log.warn("소셜 로그인 부가 정보(users 테이블) 저장 실패 — 로그인은 계속 진행 (email={})", email, e);
        }

        // JIPDAUM_USER(Django 공유 테이블)에도 사용자 존재 보장 — Order FK에 필요.
        // 반환값은 이 이메일로 JIPDAUM_USER 행이 방금 새로 생성됐는지 여부(=진짜 신규가입인지) —
        // 이메일/비밀번호로 이미 가입돼 있던 계정을 소셜로 처음 연결한 경우도 false로 잡힌다.
        boolean isNewUser = jipdaumUserProvisioner.ensureExists(
                email, registrationId + "_" + providerId, name != null ? name : "소셜사용자");

        return new CustomOAuth2User(oAuth2User, email, isNewUser);
    }
}