package com.jstudy.inout.leave.controller;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.leave.dto.AnnualLeaveDto;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "?? ?? ??", description = "?? ?? ?? ? ?? (EMPLOYEE / OWNER)")
@RestController
@RequestMapping("/api/emp/vacation")
@PreAuthorize("hasAnyRole('EMPLOYEE', 'OWNER')")
@RequiredArgsConstructor
public class AnnualLeaveEmpController {

    private final AnnualLeaveService annualLeaveService;

    @Operation(summary = "?? ??",
               description = "?? ??? ?????. ?? ?? ??? PENDING(??)???.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "?? ?? (?? ID ??)"),
            @ApiResponse(responseCode = "400", description = "?? ?? / ?? ??")
    })
    @PostMapping
    public ResponseEntity<?> submitLeave(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AnnualLeaveDto.CreateRequest request) {

        Long leaveId = annualLeaveService.submitLeave(getUserId(principal), request);
        return ResponseResult.success("?? ??? ???????.", leaveId);
    }

    @Operation(summary = "내 연차 신청 목록 조회", description = "본인이 신청한 연차 내역을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ResponseEntity<?> getMyLeaveList(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AnnualLeaveDto.ListItem> result = annualLeaveService.getMyLeaveList(getUserId(principal), pageable);
        return ResponseResult.success("연차 신청 목록 조회가 완료되었습니다.", result);
    }

    @Operation(summary = "잔여 연차 조회", description = "기본 부여 일수에서 승인 사용분을 차감한 잔여 연차를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/remaining")
    public ResponseEntity<?> getRemainingLeaveDays(@AuthenticationPrincipal CustomUserDetails principal) {
        int remaining = annualLeaveService.getRemainingLeaveDays(getUserId(principal));
        return ResponseResult.success("잔여 연차 조회가 완료되었습니다.", remaining);
    }

    @Operation(summary = "내 연차 신청 상세 조회", description = "본인이 신청한 연차 상세(반려 사유 포함)를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "본인 신청만 조회 가능"),
            @ApiResponse(responseCode = "404", description = "연차 신청 없음")
    })
    @GetMapping("/{leaveId}")
    public ResponseEntity<?> getMyLeaveDetail(
            @Parameter(description = "조회할 연차 신청 ID") @PathVariable("leaveId") Long leaveId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        AnnualLeaveDto.DetailResponse result = annualLeaveService.getMyLeaveDetail(getUserId(principal), leaveId);
        return ResponseResult.success("연차 신청 상세 조회가 완료되었습니다.", result);
    }

    private Long getUserId(CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) {
            throw new InoutException("?? ??? ???? ????.", 401, "UNAUTHORIZED");
        }
        return principal.getUser().getId();
    }
}
