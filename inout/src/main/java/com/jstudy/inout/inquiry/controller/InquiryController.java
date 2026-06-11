package com.jstudy.inout.inquiry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.inquiry.dto.InquiryCreateRequest;
import com.jstudy.inout.inquiry.dto.InquiryDetailResponse;
import com.jstudy.inout.inquiry.dto.InquiryListResponse;
import com.jstudy.inout.inquiry.service.InquiryService;

import lombok.RequiredArgsConstructor;

@Tag(name = "1:1 문의 관리", description = "가맹점 직원의 문의 등록 및 본사 관리자와의 소통을 위한 API")
@RestController
@RequestMapping("/api/inquiry")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @Operation(summary = "1:1 문의글 작성 (파일 첨부 포함)",
            description = "가맹점 직원이 본사에 시스템 오류나 건의사항을 공유하기 위해 새 문의글을 작성합니다.")
 @ApiResponses({
         @ApiResponse(responseCode = "200", description = "문의 등록 성공 — 생성된 문의 ID 반환"),
         @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (제목/본문 누락 등)")
 })
		 @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
		 @PreAuthorize("hasRole('EMPLOYEE')") 
		 public ResponseEntity<?> createInquiry(
		         @Valid @ModelAttribute InquiryCreateRequest request,
		         @AuthenticationPrincipal CustomUserDetails principal) {
		         
		     Long inquiryId = inquiryService.createInquiry(principal.getUser().getId(), request);
		     return ResponseResult.success("문의가 등록되었습니다.", inquiryId);
		 }

    @Operation(summary = "문의글 목록 조회",
               description = """
                       등록된 1:1 문의 내역을 최신순으로 페이징 조회합니다.
                       - **관리자(ADMIN):** 전체 가맹점의 모든 문의글을 조회할 수 있습니다.
                       - **직원(EMPLOYEE):** 본인이 속한 매장 또는 본인이 작성한 문의글만 필터링되어 조회됩니다.
                       """)
    @ApiResponse(responseCode = "200", description = "목록 조회 성공")
    @GetMapping
    public ResponseEntity<?> getInquiryList(
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Page<InquiryListResponse> list = inquiryService.getInquiryList(principal.getUser().getId(), isAdmin, pageable);
        return ResponseResult.success("목록 조회 성공", list);
    }

    @Operation(summary = "문의글 상세 조회",
               description = "특정 문의글의 상세 본문 내용과 하위에 등록된 답변(댓글/대댓글) 구조를 한 번에 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상세 조회 성공"),
            @ApiResponse(responseCode = "403", description = "타인의 비공개 글에 접근 시도 (권한 없음)"),
            @ApiResponse(responseCode = "404", description = "해당 문의글을 찾을 수 없음")
    })
    @GetMapping("/{inquiryId}")
    public ResponseEntity<?> getInquiryDetail(
            @Parameter(description = "조회할 문의글 ID") @PathVariable("inquiryId") Long inquiryId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        InquiryDetailResponse detail = inquiryService.getInquiryDetail(inquiryId, principal.getUser().getId(), isAdmin);
        return ResponseResult.success("상세 조회 성공", detail);
    }

    @Operation(summary = "문의글 삭제",
               description = "등록된 문의글을 삭제합니다. 작성자 본인(또는 서비스 로직에 따른 관리자)만 삭제할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "문의글 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "본인이 작성하지 않은 글 삭제 시도"),
            @ApiResponse(responseCode = "404", description = "해당 문의글을 찾을 수 없음")
    })
    @DeleteMapping("/{inquiryId}")
    public ResponseEntity<?> deleteInquiry(
            @Parameter(description = "삭제할 문의글 ID") @PathVariable("inquiryId") Long inquiryId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        inquiryService.deleteInquiry(inquiryId, principal.getUser().getId());
        return ResponseResult.successWithMessage("문의글이 삭제되었습니다.");
    }
}