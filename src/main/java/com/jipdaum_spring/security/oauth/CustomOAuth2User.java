package com.jipdaum_spring.security.oauth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User oAuth2User;
    private final String email;

    public CustomOAuth2User(OAuth2User oAuth2User, String email) {
        this.oAuth2User = oAuth2User;
        this.email = email;
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
}