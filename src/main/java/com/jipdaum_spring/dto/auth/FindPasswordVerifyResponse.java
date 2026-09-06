package com.jipdaum_spring.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

/** resetToken은 10분짜리 단기 토큰 — 다음 화면에서 새 비밀번호와 함께 그대로 돌려보내면 된다. */
public record FindPasswordVerifyResponse(@JsonProperty("reset_token") String resetToken) {
}
