package com.jipdaum_spring.dto.inquiry;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryCreateRequest(
        @NotBlank(message = "제목을 입력해주세요.") @Size(max = 200, message = "제목은 200자 이내로 입력해주세요.") String title,
        @NotBlank(message = "문의 내용을 입력해주세요.") String content
) {
}
