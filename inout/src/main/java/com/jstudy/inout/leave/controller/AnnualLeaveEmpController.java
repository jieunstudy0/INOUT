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

    @Operation(summary = "? ?? ?? ?? ??", description = "??? ??? ?? ??? ??? ?????.")
    @ApiResponse(responseCode = "200", description = "?? ??")
    @GetMapping
    public ResponseEntity<?> getMyLeaveList(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AnnualLeaveDto.ListItem> result = annualLeaveService.getMyLeaveList(getUserId(principal), pageable);
        return ResponseResult.success("?? ?? ?? ??? ???????.", result);
    }

    @Operation(summary = "? ?? ?? ?? ??", description = "??? ??? ?? ??(?? ?? ??)? ?????.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "?? ??"),
            @ApiResponse(responseCode = "403", description = "?? ??? ?? ??"),
            @ApiResponse(responseCode = "404", description = "?? ?? ??")
    })
    @GetMapping("/{leaveId}")
    public ResponseEntity<?> getMyLeaveDetail(
            @Parameter(description = "??? ?? ?? ID") @PathVariable("leaveId") Long leaveId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        AnnualLeaveDto.DetailResponse result = annualLeaveService.getMyLeaveDetail(getUserId(principal), leaveId);
        return ResponseResult.success("?? ?? ?? ??? ???????.", result);
    }

    private Long getUserId(CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) {
            throw new InoutException("?? ??? ???? ????.", 401, "UNAUTHORIZED");
        }
        return principal.getUser().getId();
    }
}
