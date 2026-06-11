package com.jstudy.inout.delivery.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.delivery.dto.DeliveryDto;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import com.jstudy.inout.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "배송 관리", description = "배송 정보 조회, 배송 시작·완료 처리 (ADMIN 전용). 발주 COMPLETED 시 자동 생성됩니다.")
@RestController
@RequestMapping("/api/admin/deliveries")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @Operation(summary = "배송 목록 조회 (페이징)",
               description = """
                       전체 배송 내역을 최신순으로 페이징 조회합니다.
                       `status` 파라미터로 상태별 필터링이 가능합니다.
                       - **READY**: 배송 준비 중 목록
                       - **SHIPPING**: 배송 중 목록
                       - **COMPLETED**: 배송 완료 목록
                       - 생략 시 전체 조회
                       """)
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ResponseEntity<?> getDeliveryList(
            @Parameter(description = "배송 상태 필터 (READY | SHIPPING | COMPLETED), 생략 시 전체 조회")
            @RequestParam(name = "status", required = false) DeliveryStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<DeliveryDto.ListItem> result = deliveryService.getDeliveryList(status, pageable);
        return ResponseResult.success("배송 목록 조회가 완료되었습니다.", result);
    }

    @Operation(summary = "배송 정보 조회",
               description = "발주 ID로 해당 발주의 배송 정보(수신자, 주소, 운송장, 상태)를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "배송 정보 없음 (발주가 COMPLETED 상태가 아니면 배송이 생성되지 않음)")
    })
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getDelivery(          
            @Parameter(description = "발주 ID") @PathVariable(name = "orderId") Long orderId) {
        DeliveryDto.DetailResponse response = deliveryService.getDeliveryByOrderId(orderId);
        return ResponseResult.success("배송 정보 조회가 완료되었습니다.", response);
    }

    @Operation(summary = "배송 시작",
               description = """
                       운송장 번호를 등록하고 배송 상태를 **READY → SHIPPING** 으로 변경합니다.
                       - `trackingNumber` 필드는 필수입니다.
                       - `shippedAt` 생략 시 현재 시각이 자동 기록됩니다.
                       - 비관적 락(PESSIMISTIC_WRITE)으로 동시 처리를 방지합니다.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "배송 시작 성공"),
            @ApiResponse(responseCode = "400", description = "운송장 번호 누락 / READY 상태가 아닌 배송"),
            @ApiResponse(responseCode = "404", description = "배송 정보 없음")
    })
    @PatchMapping("/orders/{orderId}/start")
    public ResponseEntity<?> startShipping(
            @Parameter(description = "발주 ID") @PathVariable(name = "orderId") Long orderId,
            @Valid @RequestBody DeliveryDto.StartShippingRequest request) {
        DeliveryDto.DetailResponse response = deliveryService.startShipping(orderId, request);
        return ResponseResult.success("배송 시작 처리가 완료되었습니다.", response);
    }

    @Operation(summary = "배송 완료",
               description = """
                       배송 상태를 **SHIPPING → DELIVERED** 로 변경합니다.
                       - `deliveredAt` 생략 시 현재 시각이 자동 기록됩니다.
                       - SHIPPING 상태인 배송에만 적용 가능합니다.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "배송 완료 처리 성공"),
            @ApiResponse(responseCode = "400", description = "SHIPPING 상태가 아닌 배송"),
            @ApiResponse(responseCode = "404", description = "배송 정보 없음")
    })
    @PatchMapping("/orders/{orderId}/complete")
    public ResponseEntity<?> completeDelivery(
            @Parameter(description = "발주 ID") @PathVariable(name = "orderId") Long orderId,
            @RequestBody(required = false) DeliveryDto.CompleteDeliveryRequest request) {
        DeliveryDto.DetailResponse response = deliveryService.completeDelivery(orderId, request);
        return ResponseResult.success("배송 완료 처리가 완료되었습니다.", response);
    }
}