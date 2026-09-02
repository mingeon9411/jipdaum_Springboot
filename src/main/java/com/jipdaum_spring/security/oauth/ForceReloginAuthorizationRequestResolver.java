package com.jipdaum_spring.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * 브라우저에 구글/네이버 로그인 세션이 남아있으면 인가 요청을 다시 보내도 로그인/보안인증
 * 화면 없이 바로 통과된다. provider별로 "기존 세션 무시하고 다시 로그인" 파라미터를 붙여서, 소셜
 * 재로그인 시 매번 로그인(2단계 인증 포함) 화면을 다시 띄우도록 강제한다.
 * - 구글: prompt=login (OIDC 표준 prompt 파라미터)
 * - 네이버: auth_type=reprompt (네이버 전용 파라미터)
 * - 카카오: 강제 파라미터를 붙이지 않는다. 2026-09-02에 두 차례 시도(prompt=login 제거,
 *   이후 비공식 prompt=qr_login 추가)했으나 QR 로그인은 여전히 안 됐고, 오히려 qr_login
 *   배포 후 ID/PW 로그인까지 로그인 화면으로 되돌아오는 회귀가 실사용자 리포트로 확인돼
 *   즉시 롤백함. qr_login은 카카오 공식 REST API 문서(login/none/create/select_account)에
 *   없는 미문서화 값이라, 인가 요청 시엔 통과되고 로그인 확인(POST) 단계에서 카카오 서버가
 *   재검증해 거부하며 로그인 화면으로 되돌린 것으로 추정됨(미검증). 카카오는 당분간 강제
 *   파라미터 없이 원래 인가 요청 그대로 통과시키고, 기존 세션이 남아있으면 재로그인 화면
 *   없이 바로 통과되는 것을 감수한다 — QR 로그인 자체는 카카오 웹 OAuth가 공식 지원하지
 *   않는 것으로 잠정 결론.
 */
@Component
public class ForceReloginAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String AUTHORIZATION_REQUEST_BASE_URI = "/oauth2/authorization";

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public ForceReloginAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, AUTHORIZATION_REQUEST_BASE_URI);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return withForceRelogin(delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return withForceRelogin(delegate.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest withForceRelogin(OAuth2AuthorizationRequest authorizationRequest) {
        if (authorizationRequest == null) {
            return null;
        }

        String uri = authorizationRequest.getAuthorizationRequestUri();
        String paramName;
        String paramValue;
        if (uri.contains("nid.naver.com")) {
            paramName = "auth_type";
            paramValue = "reprompt";
        } else if (uri.contains("accounts.google.com")) {
            paramName = "prompt";
            paramValue = "login";
        } else {
            return authorizationRequest;
        }

        String newUri = UriComponentsBuilder.fromUriString(uri)
                .queryParam(paramName, paramValue)
                .build()
                .toUriString();

        Map<String, Object> extraParams = new HashMap<>(authorizationRequest.getAdditionalParameters());
        extraParams.put(paramName, paramValue);

        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(extraParams)
                .authorizationRequestUri(newUri)
                .build();
    }
}