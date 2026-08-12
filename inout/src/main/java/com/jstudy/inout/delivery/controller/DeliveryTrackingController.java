package com.jstudy.inout.delivery.controller;

import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.delivery.dto.DeliveryTrackingDto;
import com.jstudy.inout.delivery.service.DeliveryTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "배송 조회 프록시", description = "서드파티 배송조회 API 프록시 (키 노출 방지). 실패 시 Mock 타임라인 Fallback.")
@RestController
@RequestMapping("/api/deliveries")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class DeliveryTrackingController {

    private final DeliveryTrackingService deliveryTrackingService;

    @Operation(summary = "배송 상태 조회",
               description = "carrier·trackingNumber로 외부 배송조회를 호출하고, 실패/Mock 송장이면 가상 타임라인을 반환합니다.")
    @GetMapping("/tracking")
    public ResponseEntity<?> track(
            @Parameter(description = "택배사명") @RequestParam(name = "carrier", required = false) String carrier,
            @Parameter(description = "운송장 번호") @RequestParam(name = "trackingNumber") String trackingNumber) {

        DeliveryTrackingDto.TrackingResponse result = deliveryTrackingService.track(carrier, trackingNumber);
        return ResponseResult.success("배송 조회가 완료되었습니다.", result);
    }
}
