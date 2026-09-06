package com.jipdaum_spring.controller;

import com.jipdaum_spring.dto.inquiry.InquiryCreateRequest;
import com.jipdaum_spring.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @GetMapping
    public ResponseEntity<?> getMyInquiries() {
        return ResponseEntity.ok(inquiryService.getMyInquiries());
    }

    @PostMapping
    public ResponseEntity<?> createInquiry(@Valid @RequestBody InquiryCreateRequest request) {
        return ResponseEntity.ok(inquiryService.createInquiry(request));
    }
}
