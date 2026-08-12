package com.jstudy.inout.inquiry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
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
import com.jstudy.inout.inquiry.entity.InquiryTargetType;
import com.jstudy.inout.inquiry.service.InquiryService;
import com.jstudy.inout.inquiry.service.InquiryService.InquiryFileResource;

import lombok.RequiredArgsConstructor;

@Tag(name = "1:1 문의 관리", description = "가맹점 직원·점주의 문의 등록 및 역할별 조회 API")
@RestController
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    // ─── 공통: 문의 작성 (직원 / 점주 모두 가능) ──────────────────────────────────
    @Operation(summary = "문의글 작성 (파일 첨부 포함)",
               description = "직원 또는 점주가 본사(ADMIN) 또는 점주(OWNER)를 대상으로 문의글을 작성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "문의 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    @PostMapping(path = "/api/inquiry", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'OWNER')")
    public ResponseEntity<?> createInquiry(
            @Valid @ModelAttribute InquiryCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long inquiryId = inquiryService.createInquiry(principal.getUser().getId(), request);
        return ResponseResult.success("문의가 등록되었습니다.", inquiryId);
    }

    // ─── 공통: 문의 상세 조회 ────────────────────────────────────────────────────
    @Operation(summary = "문의글 상세 조회",
               description = "특정 문의글의 상세 내용 + 댓글을 조회합니다. 작성자 본인, 같은 매장 점주, 또는 관리자만 가능합니다.")
    @GetMapping("/api/inquiry/{inquiryId}")
    public ResponseEntity<?> getInquiryDetail(
            @Parameter(description = "조회할 문의글 ID") @PathVariable("inquiryId") Long inquiryId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        boolean isAdmin = hasRole(principal, "ROLE_ADMIN");
        Long storeId = principal.getUser().getStore() != null
                ? principal.getUser().getStore().getId() : null;
        boolean isOwner = hasRole(principal, "ROLE_OWNER");
        InquiryDetailResponse detail = inquiryService.getInquiryDetail(
                inquiryId, principal.getUser().getId(), isAdmin, isOwner ? storeId : null);
        return ResponseResult.success("상세 조회 성공", detail);
    }

    // ─── 공통: 문의 삭제 ─────────────────────────────────────────────────────────
    @Operation(summary = "문의글 삭제")
    @DeleteMapping("/api/inquiry/{inquiryId}")
    public ResponseEntity<?> deleteInquiry(
            @PathVariable("inquiryId") Long inquiryId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        inquiryService.deleteInquiry(inquiryId, principal.getUser().getId());
        return ResponseResult.successWithMessage("문의글이 삭제되었습니다.");
    }

    // ─── 공통: 첨부파일 다운로드 ──────────────────────────────────────────────────
    @GetMapping("/api/inquiry/{inquiryId}/download")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'OWNER', 'ADMIN')")
    public ResponseEntity<Resource> downloadInquiryFile(
            @PathVariable("inquiryId") Long inquiryId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        boolean isAdmin = hasRole(principal, "ROLE_ADMIN");
        InquiryFileResource file = inquiryService.getInquiryFileResource(
                inquiryId, principal.getUser().getId(), isAdmin);
        String encodedName = URLEncoder.encode(file.originalFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.originalFileName().replace("\"", "")
                        + "\"; filename*=UTF-8''" + encodedName)
                .body(file.resource());
    }

    // ── EMP 탭별 목록 ─────────────────────────────────────────────────────────────
    @Operation(summary = "[직원] 본사 문의 목록", description = "직원이 본사(ADMIN)로 작성한 문의 목록")
    @GetMapping("/api/emp/inquiries/to-admin")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> empInquiriesToAdmin(
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Page<InquiryListResponse> list = inquiryService.getEmpInquiriesByTarget(
                principal.getUser().getId(), InquiryTargetType.ADMIN, pageable);
        return ResponseResult.success("목록 조회 성공", list);
    }

    @Operation(summary = "[직원] 점주 문의 목록", description = "직원이 점주(OWNER)로 작성한 문의 목록")
    @GetMapping("/api/emp/inquiries/to-owner")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> empInquiriesToOwner(
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Page<InquiryListResponse> list = inquiryService.getEmpInquiriesByTarget(
                principal.getUser().getId(), InquiryTargetType.OWNER, pageable);
        return ResponseResult.success("목록 조회 성공", list);
    }

    // ── OWNER 탭별 목록 ───────────────────────────────────────────────────────────
    @Operation(summary = "[점주] 매장 직원 문의 목록", description = "매장 직원이 점주에게 보낸 내부 문의 (targetType=OWNER)")
    @GetMapping("/api/owner/inquiries/from-staff")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> ownerInquiriesFromStaff(
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long storeId = requireStoreId(principal);
        Page<InquiryListResponse> list = inquiryService.getOwnerInquiriesFromStaff(storeId, pageable);
        return ResponseResult.success("목록 조회 성공", list);
    }

    @Operation(summary = "[점주] 본사 문의 내역", description = "매장(점주+직원)이 본사로 보낸 문의 (targetType=ADMIN)")
    @GetMapping("/api/owner/inquiries/to-admin")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> ownerInquiriesToAdmin(
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long storeId = requireStoreId(principal);
        Page<InquiryListResponse> list = inquiryService.getOwnerInquiriesToAdmin(storeId, pageable);
        return ResponseResult.success("목록 조회 성공", list);
    }

    // ── ADMIN 탭별 목록 ───────────────────────────────────────────────────────────
    @Operation(summary = "[관리자] 가맹점주 문의 목록", description = "점주(ROLE_OWNER)가 본사로 보낸 문의")
    @GetMapping("/api/admin/inquiries/from-owners")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminInquiriesFromOwners(Pageable pageable) {
        Page<InquiryListResponse> list = inquiryService.getAdminInquiriesFromOwners(pageable);
        return ResponseResult.success("목록 조회 성공", list);
    }

    @Operation(summary = "[관리자] 매장직원 문의 목록", description = "일반 직원(ROLE_EMPLOYEE)이 본사로 보낸 문의")
    @GetMapping("/api/admin/inquiries/from-employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminInquiriesFromEmployees(Pageable pageable) {
        Page<InquiryListResponse> list = inquiryService.getAdminInquiriesFromEmployees(pageable);
        return ResponseResult.success("목록 조회 성공", list);
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────────
    private boolean hasRole(CustomUserDetails principal, String roleName) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(roleName));
    }

    private Long requireStoreId(CustomUserDetails principal) {
        if (principal.getUser().getStore() == null) {
            throw new com.jstudy.inout.common.exception.InoutException(
                    "매장 정보가 없습니다.", 400, "STORE_NOT_FOUND");
        }
        return principal.getUser().getStore().getId();
    }
}