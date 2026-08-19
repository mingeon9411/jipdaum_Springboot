package com.jipdaum_spring.controller;

import com.jipdaum_spring.dto.chat.ChatRequest;
import com.jipdaum_spring.dto.chat.ChatResponse;
import com.jipdaum_spring.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String message = request.message() != null ? request.message().strip() : "";
        if (!StringUtils.hasText(message)) {
            return ResponseEntity.badRequest().body(new ChatResponse("메시지를 입력해주세요."));
        }
        return ResponseEntity.ok(new ChatResponse(chatService.reply(message, request.history())));
    }
}