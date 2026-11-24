package com.jstudy.inout.stock.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.AdjustRequest;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.ReceiveRequest;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.SettingsRequest;
import com.jstudy.inout.stock.service.StoreReceiptService;
import com.jstudy.inout.stock.service.StoreStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "매장 재고 (점주)", description = "소속 매장 재고 조회·조정·등록·설정, 배송 수령 확인")
@RestController
@RequestMapping("/api/owner/store-stocks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class StoreStockOwnerController {

    private final StoreStockService storeStockService;
    private final StoreReceiptService storeReceiptService;

    @Operation(summary = "매장 재고 목록")
    @GetMapping
    public ResponseEntity<?> getStoreStocks(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseResult.success("매장 재고 조회가 완료되었습니다.",
                storeStockService.getStoreStocks(principal.getUser().getId()));
    }

    @Operation(summary = "등록할 수 있는 본사 상품 목록", description = "이미 매장 재고에 있는 상품은 registered=true")
    @GetMapping("/catalog")
    public ResponseEntity<?> getCatalog(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseResult.success("상품 목록 조회가 완료되었습니다.",
                storeStockService.getCatalog(principal.getUser().getId()));
    }

    @Operation(summary = "매장 재고 조정·등록",
            description = "실제 수량으로 맞춥니다(폐기·파손·자체 구매·처음 등록). 매장에 없던 상품이면 새로 등록합니다. 사유 필수.")
    @PostMapping("/adjust")
    public ResponseEntity<?> adjust(@RequestBody @Valid AdjustRequest request,
                                    @AuthenticationPrincipal CustomUserDetails principal) {
        storeStockService.adjust(principal.getUser().getId(), request);
        return ResponseResult.successWithMessage("매장 재고가 조정되었습니다.");
    }

    @Operation(summary = "안전재고·보관 위치 설정")
    @PatchMapping("/{storeStockId}/settings")
    public ResponseEntity<?> updateSettings(@PathVariable("storeStockId") Long storeStockId,
                                            @RequestBody @Valid SettingsRequest request,
                                            @AuthenticationPrincipal CustomUserDetails principal) {
        storeStockService.updateSettings(principal.getUser().getId(), storeStockId, request);
        return ResponseResult.successWithMessage("매장 재고 설정이 변경되었습니다.");
    }

    @Operation(summary = "매장 재고 한 건 (상세 화면)")
    @GetMapping("/{storeStockId}")
    public ResponseEntity<?> getStoreStock(@PathVariable("storeStockId") Long storeStockId,
                                           @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseResult.success("매장 재고 조회가 완료되었습니다.",
                storeStockService.getStoreStock(principal.getUser().getId(), storeStockId));
    }

    @Operation(summary = "매장 재고 변경 이력 (최근 50건)")
    @GetMapping("/{storeStockId}/history")
    public ResponseEntity<?> getHistory(@PathVariable("storeStockId") Long storeStockId,
                                        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseResult.success("매장 재고 이력 조회가 완료되었습니다.",
                storeStockService.getHistory(principal.getUser().getId(), storeStockId));
    }

    @Operation(summary = "수령 확인 대기 배송")
    @GetMapping("/receipts")
    public ResponseEntity<?> getPendingReceipts(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseResult.success("수령 대기 목록 조회가 완료되었습니다.",
                storeReceiptService.getPendingReceipts(principal.getUser().getId()));
    }

    @Operation(summary = "배송 수령 확인")
    @PostMapping("/receipts/{orderId}")
    public ResponseEntity<?> receive(@PathVariable("orderId") Long orderId,
                                     @RequestBody @Valid ReceiveRequest request,
                                     @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseResult.success("수령 확인이 완료되었습니다.",
                storeReceiptService.receive(principal.getUser().getId(), orderId, request));
    }
}
