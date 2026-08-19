package com.jipdaum_spring.dto.auth;

/** 프론트는 이 ticket을 붙여 /oauth2/authorization/{provider}?ticket=... 으로 이동해야 한다. */
public record SocialCaptchaResponse(String ticket) {
}
