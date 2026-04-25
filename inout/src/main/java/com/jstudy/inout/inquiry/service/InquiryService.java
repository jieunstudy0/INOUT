package com.jstudy.inout.inquiry.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.inquiry.dto.*;
import com.jstudy.inout.inquiry.entity.Inquiry;
import com.jstudy.inout.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;

/**
 * 문의 게시판 서비스.
 *
 * [역할 구분]
 * - 직원(USER): 문의글 작성 · 본인 글 조회/삭제
 * - 관리자(ADMIN): 전체 문의 목록 조회, 상세 조회 시 읽음 처리
 *
 * 클래스 레벨 @Transactional(readOnly = true) 기본 설정.
 * 쓰기 작업 메서드는 개별 @Transactional 로 덮어씁니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    /**
     * 문의글을 작성합니다 (직원만 가능).
     *
     * @param userId  작성자 ID
     * @param request 제목·내용 요청 DTO
     * @return 저장된 문의글 ID
     * @throws InoutException 사용자 없음(404)
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
     * 문의 목록을 페이징 조회합니다.
     *
     * 관리자이면 전체 목록, 직원이면 본인 작성 글만 조회합니다.
     * N+1 방지를 위해 Fetch Join 쿼리를 사용합니다.
     *
     * @param userId   요청자 ID
     * @param isAdmin  관리자 여부
     * @param pageable 페이지 정보
     * @return 페이징된 문의 목록 응답 DTO
     */
    public Page<InquiryListResponse> getInquiryList(Long userId, boolean isAdmin, Pageable pageable) {
        Page<Inquiry> inquiries;

        if (isAdmin) {
            // N+1 방지: Fetch Join으로 작성자 정보 한 번에 조회
            inquiries = inquiryRepository.findAllWithAuthor(pageable);
        } else {
            // N+1 방지: 본인 글만, 작성자 정보 포함 조회
            inquiries = inquiryRepository.findAllWithAuthorByUserId(userId, pageable);
        }

        return inquiries.map(InquiryListResponse::from);
    }

    /**
     * 문의 상세를 조회합니다.
     *
     * 관리자가 조회하면 자동으로 읽음(isRead=true) 처리됩니다.
     * 직원은 본인 글만 조회할 수 있습니다.
     *
     * @param inquiryId 문의글 ID
     * @param userId    요청자 ID
     * @param isAdmin   관리자 여부
     * @return 문의 상세 응답 DTO (댓글 포함)
     * @throws InoutException 문의글 없음(404) / 권한 없음(403)
     */
    @Transactional
    public InquiryDetailResponse getInquiryDetail(Long inquiryId, Long userId, boolean isAdmin) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new InoutException("문의글을 찾을 수 없습니다.", 404, "INQUIRY_NOT_FOUND"));

        // 직원이 다른 사람의 글에 접근하는 경우 차단
        if (!isAdmin && !inquiry.getAuthor().getId().equals(userId)) {
            throw new InoutException("조회 권한이 없습니다.", 403, "FORBIDDEN");
        }

        // 관리자 최초 조회 시 읽음 처리 (Dirty Checking 활용)
        if (isAdmin && !inquiry.isRead()) {
            inquiry.markAsRead();
        }

        return InquiryDetailResponse.from(inquiry);
    }

    /**
     * 문의글을 삭제합니다 (본인만 가능).
     * CascadeType.ALL 로 인해 연결된 댓글도 함께 삭제됩니다.
     *
     * @param inquiryId 삭제 대상 문의글 ID
     * @param userId    요청자 ID
     * @throws InoutException 문의글 없음(404) / 권한 없음(403)
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