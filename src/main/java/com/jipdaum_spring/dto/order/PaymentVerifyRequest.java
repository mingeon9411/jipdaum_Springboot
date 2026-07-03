package com.jipdaum_spring.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PaymentVerifyRequest(
        // PortOne 결제 조회 URL 경로에 그대로 들어가므로, 슬래시/물음표 등으로 경로를
        // 조작할 수 없도록 안전한 문자만 허용한다.
        @JsonProperty("payment_id")
        @NotBlank(message = "payment_id가 필요합니다.")
        @Pattern(regexp = "^[A-Za-z0-9._-]{1,200}$", message = "payment_id 형식이 올바르지 않습니다.")
        String paymentId,

        @JsonProperty("merchant_uid") @NotBlank(message = "merchant_uid가 필요합니다.") String merchantUid
) {
}