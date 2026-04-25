package com.jstudy.inout.stock.controller;

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

@RestController
@RequestMapping("/stock/emp")
@RequiredArgsConstructor
public class StockEmpController {

    private final StockEmpService stockEmpService;

    @PostMapping("/use")
    @PreAuthorize("hasRole('EMPLOYEE')") 
    public ResponseEntity<?> useStock(@RequestBody @Valid StockUseRequest stockUseRequest
    		, @AuthenticationPrincipal CustomUserDetails principal) {

    	Long currentUserId = principal.getUser().getId();

        Long itemId = stockEmpService.useStock(
        		stockUseRequest.getItemId(), 
        		stockUseRequest.getQuantity(), 
            currentUserId, 
            stockUseRequest.getMemo()
        );

        return ResponseResult.success("재고 사용 처리가 완료되었습니다.", itemId);
        
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')") 
    public ResponseEntity<?> getEmployeeStockList(
            @RequestParam(value = "name", required = false) String name,
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable 
    ) {
       
        Page<ItemResponse> pageResult = stockEmpService.getEmployeeStockList(name, pageable);
        
        return ResponseResult.success("상품 목록 조회가 완료되었습니다.", pageResult);
        
    }

    @GetMapping("/detail/{itemId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> getStockDetail(@PathVariable Long itemId) {
    	
        StockUserDetailResponse detail = stockEmpService.getEmployeeStockDetail(itemId);
        return ResponseResult.success("재고 상세 조회가 완료되었습니다.", detail);
        
    }
    
    
    
    
}
