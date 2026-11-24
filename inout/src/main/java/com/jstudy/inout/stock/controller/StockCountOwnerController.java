package com.jstudy.inout.stock.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.stock.dto.count.StockCountApplyResponse;
import com.jstudy.inout.stock.dto.count.StockCountRejectRequest;
import com.jstudy.inout.stock.dto.count.StockCountSheetResponse;
import com.jstudy.inout.stock.dto.count.StockCountSummaryResponse;
import com.jstudy.inout.stock.entity.StockCountStatus;
import com.jstudy.inout.stock.service.StockCountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "매장 재고 실사 (점주)", description = "직원이 제출한 실재고 확인, 승인(매장 재고 반영)·반려")
@RestController
@RequestMapping("/api/owner/stock-counts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class StockCountOwnerController {

    private final StockCountService stockCountService;

    @Operation(summary = "실사표 목록", description = "소속 매장 실사표. status를 주면 그 상태만 (예: SUBMITTED = 승인 대기)")
    @GetMapping
    public ResponseEntity<?> getSheets(@RequestParam(name = "status", required = false) StockCountStatus status,
                                       @AuthenticationPrincipal CustomUserDetails principal) {
        List<StockCountSummaryResponse> sheets = stockCountService.getRecentSheets(principal.getUser().getId(), status);
        return ResponseResult.success("실사표 목록 조회가 완료되었습니다.", sheets);
    }

    @Operation(summary = "실사표 확인", description = "줄마다 출력 시점 재고, 실재고, 차이, 지금 매장 재고, 승인 후 예상 재고를 보여 줍니다.")
    @GetMapping("/{sheetId}")
    public ResponseEntity<?> getSheet(@PathVariable("sheetId") Long sheetId,
                                      @AuthenticationPrincipal CustomUserDetails principal) {
        StockCountSheetResponse sheet = stockCountService.getSheetForReview(principal.getUser().getId(), sheetId);
        return ResponseResult.success("실사표 조회가 완료되었습니다.", sheet);
    }

    @Operation(summary = "승인 (매장 재고 반영)",
            description = "줄마다 (실재고 - 출력 시점 재고)만큼 지금 매장 재고를 늘리거나 줄이고 이력을 남깁니다. "
                    + "반영 후 0보다 작아지는 상품이 하나라도 있으면 아무것도 바꾸지 않고 409로 응답합니다.")
    @PostMapping("/{sheetId}/approve")
    public ResponseEntity<?> approve(@PathVariable("sheetId") Long sheetId,
                                     @AuthenticationPrincipal CustomUserDetails principal) {
        StockCountApplyResponse result = stockCountService.approve(sheetId, principal.getUser().getId());
        return ResponseResult.success("실사 결과가 매장 재고에 반영되었습니다.", result);
    }

    @Operation(summary = "반려", description = "직원이 다시 세어 제출할 수 있도록 작성 중 상태로 되돌립니다.")
    @PostMapping("/{sheetId}/reject")
    public ResponseEntity<?> reject(@PathVariable("sheetId") Long sheetId,
                                    @RequestBody @Valid StockCountRejectRequest request,
                                    @AuthenticationPrincipal CustomUserDetails principal) {
        stockCountService.reject(sheetId, principal.getUser().getId(), request.getReason());
        return ResponseResult.successWithMessage("실사표가 반려되었습니다.");
    }
}
