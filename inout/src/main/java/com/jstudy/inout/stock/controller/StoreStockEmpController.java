package com.jstudy.inout.stock.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.ReceiveRequest;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.UseRequest;
import com.jstudy.inout.stock.service.StoreReceiptService;
import com.jstudy.inout.stock.service.StoreStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "매장 재고 (직원)", description = "소속 매장 재고 조회·사용 처리, 본사 배송 수령 확인")
@RestController
@RequestMapping("/api/emp/store-stocks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYEE')")
public class StoreStockEmpController {

    private final StoreStockService storeStockService;
    private final StoreReceiptService storeReceiptService;

    @Operation(summary = "매장 재고 목록")
    @GetMapping
    public ResponseEntity<?> getStoreStocks(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseResult.success("매장 재고 조회가 완료되었습니다.",
                storeStockService.getStoreStocks(principal.getUser().getId()));
    }

    @Operation(summary = "매장 재고 사용 처리", description = "소속 매장 재고에서 사용한 수량을 차감합니다. 본사 재고는 바뀌지 않습니다.")
    @PostMapping("/use")
    public ResponseEntity<?> use(@RequestBody @Valid UseRequest request,
                                 @AuthenticationPrincipal CustomUserDetails principal) {
        storeStockService.use(principal.getUser().getId(), request);
        return ResponseResult.successWithMessage("매장 재고 사용 처리가 완료되었습니다.");
    }

    @Operation(summary = "매장 재고 변경 이력 (최근 50건)")
    @GetMapping("/{storeStockId}/history")
    public ResponseEntity<?> getHistory(@PathVariable("storeStockId") Long storeStockId,
                                        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseResult.success("매장 재고 이력 조회가 완료되었습니다.",
                storeStockService.getHistory(principal.getUser().getId(), storeStockId));
    }

    @Operation(summary = "수령 확인 대기 배송", description = "배송 중이거나 최근 14일 안에 배송 완료됐지만 아직 수령 확인하지 않은 소속 매장 배송")
    @GetMapping("/receipts")
    public ResponseEntity<?> getPendingReceipts(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseResult.success("수령 대기 목록 조회가 완료되었습니다.",
                storeReceiptService.getPendingReceipts(principal.getUser().getId()));
    }

    @Operation(summary = "배송 수령 확인",
            description = "품목마다 실제로 받은 수량(0 ~ 발주 수량)을 입력하면 받은 만큼 매장 재고가 늘어납니다.")
    @PostMapping("/receipts/{orderId}")
    public ResponseEntity<?> receive(@PathVariable("orderId") Long orderId,
                                     @RequestBody @Valid ReceiveRequest request,
                                     @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseResult.success("수령 확인이 완료되었습니다.",
                storeReceiptService.receive(principal.getUser().getId(), orderId, request));
    }
}
