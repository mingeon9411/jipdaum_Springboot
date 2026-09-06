package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.inquiry.Inquiry;
import com.jipdaum_spring.domain.inquiry.InquiryRepository;
import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;
import com.jipdaum_spring.dto.inquiry.InquiryCreateRequest;
import com.jipdaum_spring.dto.inquiry.InquiryResponse;
import com.jipdaum_spring.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final CurrentUserProvider currentUserProvider;

    public List<InquiryResponse> getMyInquiries() {
        JipdaumUser user = currentUserProvider.getCurrentUser();
        return inquiryRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(InquiryResponse::from)
                .toList();
    }

    @Transactional
    public InquiryResponse createInquiry(InquiryCreateRequest request) {
        JipdaumUser user = currentUserProvider.getCurrentUser();
        Inquiry inquiry = new Inquiry();
        inquiry.setUser(user);
        inquiry.setTitle(request.title());
        inquiry.setContent(request.content());
        inquiry.setCreatedAt(LocalDateTime.now());
        return InquiryResponse.from(inquiryRepository.save(inquiry));
    }
}
