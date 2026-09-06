package com.jipdaum_spring.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SecurityQuestionResponse(@JsonProperty("security_question") String securityQuestion) {
}
