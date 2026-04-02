package com.jstudy.inout.stock.controller;

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
import com.jstudy.inout.stock.dto.admin.StockDetailResponse;
import com.jstudy.inout.stock.dto.admin.StockReceiveRequest;
import com.jstudy.inout.stock.dto.admin.StockRegister;
import com.jstudy.inout.stock.dto.admin.StockUpdate;
import com.jstudy.inout.stock.dto.emp.StockHistoryResponse;
import com.jstudy.inout.stock.service.StockAdmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/stock/adm")
@RequiredArgsConstructor
@Slf4j
@Validated 
public class StockAdmController {

    private final StockAdmService stockAdmService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')") 
    public ResponseEntity<?> registerItem(@RequestBody @Valid StockRegister stockRegister) {
        
        log.info("관리자 상품 등록 요청 시작: 상품명={}", stockRegister.getName());

        Long savedItemId = stockAdmService.registerStock(stockRegister);

        log.info("관리자 상품 등록 완료: ID={}", savedItemId);

        return ResponseResult.success("상품 등록이 완료되었습니다.", savedItemId);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateItem(
            @PathVariable("id") Long id, 
            @RequestBody @Valid StockUpdate stockUpdate,
            @AuthenticationPrincipal CustomUserDetails principal 
    ) {

        Long currentUserId = principal.getUser().getId(); 

        stockAdmService.updateStock(id, stockUpdate, currentUserId);
        
        return ResponseResult.success("상품 정보가 수정되었습니다.", id);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteItem(@PathVariable("id") Long id) {
    	
        log.info("상품 삭제 요청: ID={}", id);
        
        stockAdmService.deleteStock(id);
        
        return ResponseResult.success("상품이 삭제되었습니다.", id);
        
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAdminItemList(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "deleted", defaultValue = "false") boolean deleted,
            // sort를 itemId 또는 createdAt으로 변경
            @PageableDefault(size = 10, sort = "itemId", direction = Sort.Direction.DESC) Pageable pageable 
    ) {
    	
        log.info("관리자 재고 목록 조회: 검색어={}, 페이징={}", name, pageable);
        
        Page<StockAdminResponse> pageResult = stockAdmService.getAdminStockList(name, deleted, pageable);
        
        return ResponseResult.success("재고 목록 조회가 완료되었습니다.", pageResult);
        
    }
    
    @PostMapping("/receive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> receiveStock(
        @RequestBody @Valid StockReceiveRequest stockReceiveRequest,
        @AuthenticationPrincipal CustomUserDetails principal 
    ) {

        Long currentAdminId = principal.getUser().getId();

        Long itemId = stockAdmService.receiveStock(
        		stockReceiveRequest.getItemId(), 
        		stockReceiveRequest.getQuantity(), 
            currentAdminId, 
            stockReceiveRequest.getMemo()
        );

        return ResponseResult.success("재고 입고가 완료되었습니다.", itemId);
    }

    @GetMapping("/history/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStockHistory(
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "0") int page, 
            @RequestParam(defaultValue = "20") int size  
    ) {
        List<StockHistoryResponse> history = stockAdmService.getUnifiedHistory(itemId, page, size);
        return ResponseResult.success("이력 조회가 완료되었습니다.", history);
    }

    @GetMapping("/alerts/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getLowStockAlerts() {
        List<StockAdminResponse> alerts = stockAdmService.getLowStockAlerts();
        String message = alerts.isEmpty() ? "적정 재고가 유지되고 있습니다." : "재고 보충이 필요한 상품이 있습니다.";
        return ResponseResult.success(message, alerts);
    }

    @GetMapping("/detail/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> getStockDetail(
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "0") int page,  
            @RequestParam(defaultValue = "20") int size  
    ) {
        StockDetailResponse detail = stockAdmService.getStockDetail(itemId, page, size);
        return ResponseResult.success("재고 상세 조회가 완료되었습니다.", detail);
    }
    
    
}
