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
 * 브라우저에 카카오/구글/네이버 로그인 세션이 남아있으면 인가 요청을 다시 보내도 로그인/보안인증
 * 화면 없이 바로 통과된다. provider별로 "기존 세션 무시하고 다시 로그인" 파라미터를 붙여서, 소셜
 * 재로그인 시 매번 로그인(2단계 인증/QR 포함) 화면을 다시 띄우도록 강제한다.
 * - 카카오/구글: prompt=login (OIDC 표준 prompt 파라미터, 카카오도 동일하게 지원)
 * - 네이버: auth_type=reprompt (네이버 전용 파라미터)
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
        } else if (uri.contains("kauth.kakao.com") || uri.contains("accounts.google.com")) {
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