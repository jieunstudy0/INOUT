package com.jstudy.inout.order.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.order.dto.BulkOrderRequest;
import com.jstudy.inout.order.dto.BulkOrderResponse;
import com.jstudy.inout.order.dto.OrderAdminResponse;
import com.jstudy.inout.order.dto.OrderProcessRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.service.OrderAdmService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import java.io.IOException;
import com.jstudy.inout.common.exception.InoutException;


@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')") 
@RequiredArgsConstructor
public class OrderAdmController {

    private final OrderAdmService orderAdmService;

    @GetMapping("/list")
    public ResponseEntity<?> getAllOrderRequests(
            @RequestParam(required = false) OrderStatus status) {  

        List<OrderAdminResponse> orders = orderAdmService.getAllOrders(status);
        
        return ResponseEntity.ok(ResponseResult.success(orders));
    }

    @PatchMapping("/{orderId}/process")
    public ResponseEntity<?> processOrderDetail(
            @PathVariable Long orderId,
            @RequestBody OrderProcessRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        orderAdmService.processOrderItems(orderId, request, principal.getUser().getId());
        return ResponseEntity.ok(ResponseResult.success("발주 상세 처리가 완료되었습니다."));
    }

    @PostMapping("/bulk-approve")
    public ResponseEntity<?> bulkApprove(
            @RequestBody BulkOrderRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {

        BulkOrderResponse response = orderAdmService.bulkApproveOrders(request, principal.getUser().getId());        
        return ResponseEntity.ok(ResponseResult.success("일괄 승인 처리가 완료되었습니다.", response));
    }

    @GetMapping("/orders/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public void downloadExcel(HttpServletResponse response) {
        try {
            orderAdmService.exportOrdersToExcel(response);
        } catch (IOException e) {
            throw new InoutException("엑셀 파일 생성 중 오류가 발생했습니다.", 500, "EXCEL_ERROR");
        }
    }
}
