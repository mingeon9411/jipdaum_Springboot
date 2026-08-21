package com.jipdaum_spring.security.oauth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User oAuth2User;
    private final String email;
    private final boolean newUser;

    public CustomOAuth2User(OAuth2User oAuth2User, String email, boolean newUser) {
        this.oAuth2User = oAuth2User;
        this.email = email;
        this.newUser = newUser;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oAuth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oAuth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return oAuth2User.getName();
    }

    public String getEmail() {
        return email;
    }

    /** 이 로그인으로 JIPDAUM_USER 행이 방금 새로 생성됐는지(true=신규가입, false=기존 회원 로그인). */
    public boolean isNewUser() {
        return newUser;
    }
}