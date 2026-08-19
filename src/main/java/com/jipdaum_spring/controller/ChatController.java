package com.jipdaum_spring.controller;

import com.jipdaum_spring.dto.chat.ChatRequest;
import com.jipdaum_spring.dto.chat.ChatResponse;
import com.jipdaum_spring.security.captcha.HCaptchaVerifier;
import com.jipdaum_spring.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final HCaptchaVerifier hCaptchaVerifier;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String message = request.message() != null ? request.message().strip() : "";
        if (!StringUtils.hasText(message)) {
            return ResponseEntity.badRequest().body(new ChatResponse("메시지를 입력해주세요."));
        }

        // 봇이 새 대화를 계속 만들어 비용을 태우는 걸 막기 위해, 대화 시작(history 없음) 시점에만
        // hCaptcha를 확인한다. 같은 대화의 후속 메시지는 재검증하지 않는다 — Bucket4j rate limit이
        // 그 구간의 남용을 막아준다(ChatRateLimitFilter 참고).
        boolean isNewConversation = CollectionUtils.isEmpty(request.history());
        if (isNewConversation && !hCaptchaVerifier.verify(request.captchaToken())) {
            return ResponseEntity.badRequest().body(new ChatResponse("보안 인증에 실패했습니다. 다시 시도해주세요."));
        }

        return ResponseEntity.ok(new ChatResponse(chatService.reply(message, request.history())));
    }
}