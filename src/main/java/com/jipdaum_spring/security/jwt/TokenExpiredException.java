package com.jipdaum_spring.security.jwt;

/** 로그인이 필요한 API에서 access token이 만료된 채로 들어왔을 때 던진다. */
public class TokenExpiredException extends RuntimeException {

    public TokenExpiredException() {
        super("Access token has expired");
    }
}