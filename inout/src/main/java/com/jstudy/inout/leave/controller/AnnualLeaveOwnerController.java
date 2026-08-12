package com.jstudy.inout.leave.controller;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.leave.dto.AnnualLeaveDto;
import com.jstudy.inout.leave.entity.LeaveStatus;
import com.jstudy.inout.leave.service.AnnualLeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "가맹점주 연차 관리", description = "소속 매장 직원 연차 조회 및 승인·반려·보류 (OWNER 전용)")
@RestController
@RequestMapping("/api/owner/vacation")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
public class AnnualLeaveOwnerController {

    private final AnnualLeaveService annualLeaveService;

    @Operation(summary = "소속 매장 연차 신청 목록 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ResponseEntity<?> getLeaveList(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "연차 상태 필터")
            @RequestParam(name = "status", required = false) LeaveStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long storeId = requireStoreId(principal);
        Page<AnnualLeaveDto.ListItem> result = annualLeaveService.getLeaveListByStore(storeId, status, pageable);
        return ResponseResult.success("연차 신청 목록 조회가 완료되었습니다.", result);
    }

    @Operation(summary = "소속 매장 연차 신청 상세 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "타 매장 접근"),
            @ApiResponse(responseCode = "404", description = "연차 신청 없음")
    })
    @GetMapping("/{leaveId}")
    public ResponseEntity<?> getLeaveDetail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("leaveId") Long leaveId) {

        Long storeId = requireStoreId(principal);
        AnnualLeaveDto.DetailResponse result = annualLeaveService.getLeaveDetailByStore(storeId, leaveId);
        return ResponseResult.success("연차 신청 상세 조회가 완료되었습니다.", result);
    }

    @Operation(summary = "연차 신청 승인·반려·보류 처리")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "이미 처리됨 / 반려 사유 누락"),
            @ApiResponse(responseCode = "403", description = "타 매장 접근"),
            @ApiResponse(responseCode = "404", description = "연차 신청 없음")
    })
    @PatchMapping("/{leaveId}")
    public ResponseEntity<?> processLeave(
            @PathVariable("leaveId") Long leaveId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AnnualLeaveDto.ProcessRequest request) {

        AnnualLeaveDto.DetailResponse result =
                annualLeaveService.processLeave(leaveId, getUserId(principal), request);
        return ResponseResult.success("연차 신청 처리가 완료되었습니다.", result);
    }

    private Long requireStoreId(CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null || principal.getUser().getStore() == null) {
            throw new InoutException("소속 매장 정보가 없습니다.", 403, "STORE_REQUIRED");
        }
        return principal.getUser().getStore().getId();
    }

    private Long getUserId(CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) {
            throw new InoutException("인증 정보가 유효하지 않습니다.", 401, "UNAUTHORIZED");
        }
        return principal.getUser().getId();
    }
}
