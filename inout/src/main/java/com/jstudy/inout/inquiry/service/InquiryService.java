package com.jstudy.inout.inquiry.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.inquiry.dto.InquiryCreateRequest;
import com.jstudy.inout.inquiry.dto.InquiryDetailResponse;
import com.jstudy.inout.inquiry.dto.InquiryListResponse;
import com.jstudy.inout.inquiry.entity.Inquiry;
import com.jstudy.inout.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    /**
     * 문의글 작성 (직원만 가능)
     */
    @Transactional
    public Long createInquiry(Long userId, InquiryCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404, "USER_NOT_FOUND"));

        Inquiry inquiry = Inquiry.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .author(user)
                .build();

        return inquiryRepository.save(inquiry).getId();
    }

    /**
     * 문의 목록 조회
     * - 관리자: 전체 목록 조회 가능
     * - 직원: 본인 작성 목록만 조회 가능
     */
    public Page<InquiryListResponse> getInquiryList(Long userId, boolean isAdmin, Pageable pageable) {
        Page<Inquiry> inquiries;

        if (isAdmin) {
            inquiries = inquiryRepository.findAllWithAuthor(pageable);
        } else {          
            inquiries = inquiryRepository.findAllWithAuthorByUserId(userId, pageable);
        }
        return inquiries.map(InquiryListResponse::from);
    }

    /**
     * 문의 상세 조회
     * - 관리자: 모든 문의글 조회 가능
     * - 직원: 본인 문의글만 조회 가능
     */
    @Transactional
    public InquiryDetailResponse getInquiryDetail(Long inquiryId, Long userId, boolean isAdmin) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new InoutException("문의글을 찾을 수 없습니다.", 404, "INQUIRY_NOT_FOUND"));

        if (!isAdmin && !inquiry.getAuthor().getId().equals(userId)) {
            throw new InoutException("조회 권한이 없습니다.", 403, "FORBIDDEN");
        }
        
        if (isAdmin && !inquiry.isRead()) {
            inquiry.markAsRead();
        }
        return InquiryDetailResponse.from(inquiry);
    }

    /**
     * 문의글 삭제 (본인만 삭제 가능)
     */
    @Transactional
    public void deleteInquiry(Long inquiryId, Long userId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new InoutException("문의글을 찾을 수 없습니다.", 404, "INQUIRY_NOT_FOUND"));

        if (!inquiry.getAuthor().getId().equals(userId)) {
            throw new InoutException("삭제 권한이 없습니다.", 403, "FORBIDDEN");
        }

        inquiryRepository.delete(inquiry);
    }
}