package com.jstudy.inout.leave.controller;

import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.leave.dto.AnnualLeaveDto;
import com.jstudy.inout.leave.entity.LeaveStatus;
import com.jstudy.inout.leave.service.AnnualLeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 연차 모니터링", description = "전 직원 연차 현황 Read-Only 조회 (ADMIN 전용, 승인/반려 불가)")
@RestController
@RequestMapping("/api/admin/vacation")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AnnualLeaveAdmController {

    private final AnnualLeaveService annualLeaveService;

    @Operation(summary = "연차 신청 목록 조회 (모니터링)",
               description = "전사 직원의 연차 신청 내역을 조회합니다. 승인/반려는 가맹점주(OWNER)만 가능합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ResponseEntity<?> getLeaveList(
            @Parameter(description = "연차 상태 필터, 생략 시 전체 조회")
            @RequestParam(name = "status", required = false) LeaveStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AnnualLeaveDto.ListItem> result = annualLeaveService.getLeaveList(status, pageable);
        return ResponseResult.success("연차 신청 목록 조회가 완료되었습니다.", result);
    }

    @Operation(summary = "연차 신청 상세 조회 (모니터링)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "연차 신청 없음")
    })
    @GetMapping("/{leaveId}")
    public ResponseEntity<?> getLeaveDetail(
            @Parameter(description = "조회할 연차 신청 ID") @PathVariable("leaveId") Long leaveId) {
        AnnualLeaveDto.DetailResponse result = annualLeaveService.getLeaveDetail(leaveId);
        return ResponseResult.success("연차 신청 상세 조회가 완료되었습니다.", result);
    }
}
