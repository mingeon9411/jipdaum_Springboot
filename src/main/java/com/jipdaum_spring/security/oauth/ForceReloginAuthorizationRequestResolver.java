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
 * 브라우저에 구글/네이버/카카오 로그인 세션이 남아있으면 인가 요청을 다시 보내도 로그인/보안인증
 * 화면 없이 바로 통과된다. provider별로 "기존 세션 무시하고 다시 로그인" 파라미터를 붙여서, 소셜
 * 재로그인 시 매번 로그인(2단계 인증 포함) 화면을 다시 띄우도록 강제한다.
 * - 구글: prompt=login (OIDC 표준 prompt 파라미터)
 * - 네이버: auth_type=reprompt (네이버 전용 파라미터)
 * - 카카오: prompt=qr_login. 카카오 공식 REST API 문서(login/none/create/select_account)에는
 *   없는 값이지만, 카카오 담당자가 개발자 포럼(devtalk.kakao.com/t/sdk-qr/146595)에서
 *   "웹 환경에서 REST 방식으로 로그인 요청 시 prompt=qr_login을 요청하면 QR인증을 사용할 수
 *   있다"고 답변한 근거로 추가함(2026-09-02, 카카오톡 QR 로그인이 안 된다는 실사용자
 *   리포트 대응). 이전에는 prompt=login을 썼으나 QR 위젯을 가릴 수 있어 제외했었는데,
 *   qr_login 자체가 QR 노출용 값이라는 게 확인되어 이걸로 교체. 비공식/미문서화 값이라
 *   카카오 쪽 사양 변경 시 조용히 무시될 수 있음 — 배포 후 실제 QR 노출 여부 재확인 필요.
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
        } else if (uri.contains("kauth.kakao.com")) {
            paramName = "prompt";
            paramValue = "qr_login";
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