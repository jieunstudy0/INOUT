package com.jstudy.inout.stock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.stock.dto.admin.StockAdminResponse;
import com.jstudy.inout.stock.dto.admin.StockAdmRequest;
import com.jstudy.inout.stock.dto.admin.StockAdjustRequest;
import com.jstudy.inout.stock.dto.admin.StockDetailResponse;
import com.jstudy.inout.stock.dto.admin.StockReceiveRequest;
import com.jstudy.inout.stock.dto.admin.StockRegister;
import com.jstudy.inout.stock.dto.admin.StockUpdate;
import com.jstudy.inout.stock.dto.emp.StockHistoryResponse;
import com.jstudy.inout.stock.service.StockAdmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "관리자 재고 관리", description = "상품 등록·수정·삭제, 입고 처리, 재고 조정, 이력 조회, 저재고 알림 (ADMIN 전용)")
@RestController
@RequestMapping("/api/admin/stocks")
@RequiredArgsConstructor
@Slf4j
@Validated
public class StockAdmController {

    private final StockAdmService stockAdmService;

    @Operation(summary = "상품 등록",
               description = "카테고리 ID, 상품명, 단가, 최소 재고 기준치 등을 입력해 신규 상품을 등록합니다. 등록 직후 재고는 0입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 성공 — 생성된 상품 ID 반환"),
            @ApiResponse(responseCode = "400", description = "상품명 중복"),
            @ApiResponse(responseCode = "404", description = "카테고리 없음")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registerItem(@RequestBody @Valid StockRegister stockRegister) {
        log.info("관리자 상품 등록 요청 시작: 상품명={}", stockRegister.getName());
        Long savedItemId = stockAdmService.registerStock(stockRegister);
        log.info("관리자 상품 등록 완료: ID={}", savedItemId);
        return ResponseResult.success("상품 등록이 완료되었습니다.", savedItemId);
    }

    @Operation(summary = "상품 정보 수정",
               description = "상품명, 카테고리, 단가, 최소 재고 기준치, 단위 설명을 수정합니다. 재고 수량은 변경되지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "상품 없음 / 카테고리 없음")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateItem(
            @Parameter(description = "수정할 상품 ID") @PathVariable("id") Long id,
            @RequestBody @Valid StockUpdate stockUpdate,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        stockAdmService.updateStock(id, stockUpdate, principal.getUser().getId());
        return ResponseResult.success("상품 정보가 수정되었습니다.", id);
    }

    @Operation(summary = "상품 논리 삭제",
               description = "상품을 물리적으로 삭제하지 않고 `deleted=true`로 표시합니다. 삭제된 상품은 직원 목록에 노출되지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "이미 삭제된 상품"),
            @ApiResponse(responseCode = "404", description = "상품 없음")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteItem(
            @Parameter(description = "삭제할 상품 ID") @PathVariable("id") Long id) {
        log.info("상품 삭제 요청: ID={}", id);
        stockAdmService.deleteStock(id);
        return ResponseResult.success("상품이 삭제되었습니다.", id);
    }

    @Operation(summary = "관리자 재고 목록 조회",
               description = "상품명 검색과 삭제 여부 필터를 조합해 페이징 조회합니다. `deleted=true` 로 삭제된 상품만 조회할 수 있습니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAdminItemList(
            @Parameter(description = "상품명 검색어 (부분 일치)") @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "삭제 상품 포함 여부 (기본값: false)") @RequestParam(value = "deleted", defaultValue = "false") boolean deleted,
            @PageableDefault(size = 10, sort = "itemId", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("관리자 재고 목록 조회: 검색어={}, 페이징={}", name, pageable);
        Page<StockAdminResponse> pageResult = stockAdmService.getAdminStockList(name, deleted, pageable);
        return ResponseResult.success("재고 목록 조회가 완료되었습니다.", pageResult);
    }

    @Operation(summary = "재고 입고 처리",
               description = """
                       상품에 수량을 추가하고 입고 이력을 기록합니다.
                       **주의:** 현재 입고는 일반 `findById`를 사용합니다. 동시 입고와 승인이 겹치는 경우
                       낙관적 락(@Version) 충돌로 409 응답이 발생할 수 있습니다.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "입고 성공 — 상품 ID 반환"),
            @ApiResponse(responseCode = "404", description = "상품 없음")
    })
    @PostMapping("/receive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> receiveStock(
            @RequestBody @Valid StockReceiveRequest stockReceiveRequest,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long itemId = stockAdmService.receiveStock(
                stockReceiveRequest.getItemId(),
                stockReceiveRequest.getQuantity(),
                principal.getUser().getId(),
                stockReceiveRequest.getMemo()
        );
        return ResponseResult.success("재고 입고가 완료되었습니다.", itemId);
    }

    @Operation(summary = "재고 통합 이력 조회",
               description = "입고 이력과 사용 이력을 합산하여 최신순으로 반환합니다. 메모리 페이징 방식으로 처리됩니다.")
    @ApiResponse(responseCode = "200", description = "이력 조회 성공")
    @GetMapping("/{itemId}/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStockHistory(
            @Parameter(description = "이력 조회할 상품 ID") @PathVariable Long itemId,
            @Parameter(description = "페이지 번호 (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size
    ) {
        List<StockHistoryResponse> history = stockAdmService.getUnifiedHistory(itemId, page, size);
        return ResponseResult.success("이력 조회가 완료되었습니다.", history);
    }

    @Operation(summary = "저재고 알림 목록 조회",
               description = "`currentStock ≤ minStockLevel` 조건인 상품 목록을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/alerts/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getLowStockAlerts() {
        List<StockAdminResponse> alerts = stockAdmService.getLowStockAlerts();
        String message = alerts.isEmpty() ? "적정 재고가 유지되고 있습니다." : "재고 보충이 필요한 상품이 있습니다.";
        return ResponseResult.success(message, alerts);
    }

    @Operation(summary = "상품 상세 + 이력 + 집계 조회",
               description = "상품 기본 정보, 페이징된 통합 이력, 총 입고량/사용량 집계를 한 번에 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "상품 없음")
    })
    @GetMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<?> getStockDetail(
            @Parameter(description = "조회할 상품 ID") @PathVariable Long itemId,
            @Parameter(description = "페이지 번호 (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size
    ) {
        StockDetailResponse detail = stockAdmService.getStockDetail(itemId, page, size);
        return ResponseResult.success("재고 상세 조회가 완료되었습니다.", detail);
    }

    @Operation(summary = "재고 실사 (재고 조정)",
               description = """
                       실제 재고 수량(adjustedQuantity)을 입력하면 현재 DB 재고와의 차이를 계산하여 자동으로 이력을 기록합니다.
                       비관적 락(PESSIMISTIC_WRITE)을 사용하여 동시 수정을 방지합니다.
                       - **차이 > 0**: 입고 이력으로 기록 (재고 증가)
                       - **차이 < 0**: 사용 이력으로 기록 (재고 감소)
                       - **차이 = 0**: 변경 없음, 즉시 반환
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재고 실사 완료"),
            @ApiResponse(responseCode = "400", description = "조정 수량 미입력(0 미만) 또는 사유 없음"),
            @ApiResponse(responseCode = "404", description = "상품 없음")
    })
    @PatchMapping("/{itemId}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adjustStock(
            @Parameter(description = "실사할 상품 ID") @PathVariable Long itemId,
            @RequestBody @Valid StockAdjustRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        StockAdmRequest admRequest = StockAdmRequest.builder()
                .itemId(itemId)
                .actualStock(request.getAdjustedQuantity())
                .reason(request.getReason())
                .build();
        stockAdmService.adjustStock(principal.getUser().getId(), admRequest);
        return ResponseResult.successWithMessage("재고 실사가 완료되었습니다.");
    }
}
