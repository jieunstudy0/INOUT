package com.jstudy.inout.stock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Page;
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
import com.jstudy.inout.stock.dto.emp.ItemResponse;
import com.jstudy.inout.stock.dto.emp.StockUseRequest;
import com.jstudy.inout.stock.dto.emp.StockUserDetailResponse;
import com.jstudy.inout.stock.service.StockEmpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "직원 재고 조회·사용", description = "상품 목록/상세 조회, 재고 사용 기록 (EMPLOYEE / ADMIN)")
@RestController
@RequestMapping("/api/emp/stocks")
@RequiredArgsConstructor
public class StockEmpController {

    private final StockEmpService stockEmpService;

    @Operation(summary = "재고 사용 처리",
               description = """
                       재고에서 수량을 차감하고 사용 이력을 기록합니다.
                       비관적 락(PESSIMISTIC_WRITE)으로 동시 차감을 방지합니다.
                       삭제된 상품에는 사용 처리를 할 수 없습니다.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용 처리 성공 — 상품 ID 반환"),
            @ApiResponse(responseCode = "400", description = "재고 부족"),
            @ApiResponse(responseCode = "404", description = "상품 없음 또는 삭제된 상품")
    })
    @PostMapping("/use")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> useStock(
            @RequestBody @Valid StockUseRequest stockUseRequest,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Long itemId = stockEmpService.useStock(
                stockUseRequest.getItemId(),
                stockUseRequest.getQuantity(),
                principal.getUser().getId(),
                stockUseRequest.getMemo()
        );
        return ResponseResult.success("재고 사용 처리가 완료되었습니다.", itemId);
    }

    @Operation(summary = "직원용 재고 목록 조회",
               description = "삭제되지 않은 상품만 조회합니다. 상품명으로 부분 검색이 가능합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> getEmployeeStockList(
            @Parameter(description = "상품명 검색어 (부분 일치, 생략 시 전체)") @RequestParam(value = "name", required = false) String name,
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<ItemResponse> pageResult = stockEmpService.getEmployeeStockList(name, pageable);
        return ResponseResult.success("상품 목록 조회가 완료되었습니다.", pageResult);
    }

    @Operation(summary = "AI 스마트 발주 추천",
               description = "최근 7일 판매 속도와 안전재고를 분석하여 발주가 시급한 상품과 추천 수량을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "추천 목록 반환")
    @GetMapping("/ai-suggestions")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> getAiStockSuggestions(
            @RequestParam(name = "limit", defaultValue = "8") int limit) {
        return ResponseResult.success("AI 스마트 발주 추천을 불러왔습니다.",
                stockEmpService.getAiStockSuggestions(limit));
    }

    @Operation(summary = "직원용 상품 상세 조회",
               description = "상품의 기본 정보와 현재 재고를 반환합니다. 삭제된 상품은 조회되지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "상품 없음 또는 삭제된 상품")
    })
    @GetMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> getStockDetail(
            @Parameter(description = "조회할 상품 ID") @PathVariable("itemId") Long itemId) {
        StockUserDetailResponse detail = stockEmpService.getEmployeeStockDetail(itemId);
        return ResponseResult.success("재고 상세 조회가 완료되었습니다.", detail);
    }
}