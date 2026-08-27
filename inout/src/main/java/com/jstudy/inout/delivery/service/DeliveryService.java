package com.jstudy.inout.delivery.service;

import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.delivery.dto.DeliveryDto;
import com.jstudy.inout.delivery.entity.Delivery;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import com.jstudy.inout.delivery.repository.DeliveryRepository;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    public static final String CARRIER_CJ = "CJ대한통운";

    private final DeliveryRepository deliveryRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void createDeliveryIfAbsentForCompletedOrder(OrderRequest order) {
        if (order == null || order.getId() == null) {
            return;
        }
        if (order.getStatus() != OrderStatus.COMPLETED && order.getStatus() != OrderStatus.APPROVED) {
            return;
        }
        if (deliveryRepository.findByOrderRequest_Id(order.getId()).isPresent()) {
            return;
        }
        if (!StringUtils.hasText(order.getReceiverName())
                || !StringUtils.hasText(order.getReceiverPhone())
                || !StringUtils.hasText(order.getDestinationAddress())) {
            throw new InoutException(
                    "발주에 저장된 배송 정보가 없습니다.", 400, "ORDER_SHIPPING_SNAPSHOT_MISSING");
        }
        Delivery delivery = Delivery.builder()
                .orderRequest(order)
                .receiverName(order.getReceiverName().trim())
                .receiverPhone(order.getReceiverPhone().trim())
                .destinationAddress(order.getDestinationAddress().trim())
                .build();
        deliveryRepository.save(delivery);
    }

    public Page<DeliveryDto.ListItem> getDeliveryList(DeliveryStatus status, Pageable pageable) {
        Page<Delivery> page = (status != null)
                ? deliveryRepository.findByStatusWithOrder(status, pageable)
                : deliveryRepository.findAllWithOrder(pageable);
        return page.map(DeliveryDto::toListItem);
    }

    public Page<DeliveryDto.ListItem> getMyDeliveryList(Long userId, DeliveryStatus status, Pageable pageable) {
        Page<Delivery> page = (status != null)
                ? deliveryRepository.findByUserIdAndStatusWithOrder(userId, status, pageable)
                : deliveryRepository.findByUserIdWithOrder(userId, pageable);
        return page.map(DeliveryDto::toListItem);
    }

    public Page<DeliveryDto.ListItem> getStoreDeliveryList(Long storeId, DeliveryStatus status, Pageable pageable) {
        Page<Delivery> page = (status != null)
                ? deliveryRepository.findByStoreIdAndStatusWithOrder(storeId, status, pageable)
                : deliveryRepository.findByStoreIdWithOrder(storeId, pageable);
        return page.map(DeliveryDto::toListItem);
    }

    public DeliveryDto.DetailResponse getDeliveryByOrderId(Long orderId) {
        Delivery delivery = deliveryRepository.findByOrderRequest_Id(orderId)
                .orElseThrow(() -> new InoutException("배송 정보를 찾을 수 없습니다.", 404, "DELIVERY_NOT_FOUND"));
        return DeliveryDto.from(delivery);
    }

    /**
     * 택배사 연동 Mock 운송장 발급.
     * 외부 연동 지연을 모방한 후 CJ 형식(56 + 10자리) 송장번호를 저장한다.
     */
    @Transactional
    public DeliveryDto.DetailResponse generateWaybill(Long deliveryId) {
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InoutException("운송장 발급이 중단되었습니다.", 500, "WAYBILL_INTERRUPTED");
        }

        Delivery delivery = deliveryRepository.findByIdForUpdate(deliveryId)
                .orElseThrow(() -> new InoutException("배송 정보를 찾을 수 없습니다.", 404, "DELIVERY_NOT_FOUND"));

        if (delivery.getStatus() != DeliveryStatus.READY) {
            throw new InoutException("배송 준비 상태에서만 운송장 발급이 가능합니다.", 400, "INVALID_DELIVERY_STATUS");
        }

        String trackingNumber = generateCjStyleTrackingNumber();
        delivery.assignWaybill(CARRIER_CJ, trackingNumber);
        log.info("[운송장 Mock 발급] deliveryId={}, trackingNumber={}", deliveryId, trackingNumber);
        return DeliveryDto.from(delivery);
    }

    private String generateCjStyleTrackingNumber() {
        StringBuilder sb = new StringBuilder("56");
        for (int i = 0; i < 10; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * AI 본사 자동발주 승인 직후 배송/입고 대기 상태를 명시하기 위해
     * 가상 송장과 함께 SHIPPING 상태로 전환한다.
     */
    @Transactional
    public void markAiInboundWaiting(Long orderId) {
        Delivery delivery = deliveryRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new InoutException("배송 정보를 찾을 수 없습니다.", 404, "DELIVERY_NOT_FOUND"));
        if (delivery.getStatus() != DeliveryStatus.READY) {
            return;
        }
        String trackingNumber = generateCjStyleTrackingNumber();
        delivery.startShipping("가상 공급처 운송", trackingNumber, LocalDateTime.now());
    }

    /**
     * 기존 SHIPPING/COMPLETED 배송의 구형 송장(CJ…)·미설정 택배사를
     * CJ대한통운 + 56xxxxxxxxxx Mock 형식으로 일괄 정규화한다.
     */
    @Transactional
    public int backfillMockWaybills() {
        int updated = 0;
        for (Delivery delivery : deliveryRepository.findAll()) {
            if (delivery.getStatus() == DeliveryStatus.READY) {
                continue;
            }
            boolean needsCarrier = !StringUtils.hasText(delivery.getCarrier());
            boolean needsNumber = !StringUtils.hasText(delivery.getTrackingNumber())
                    || !delivery.getTrackingNumber().matches("^56\\d{10}$");
            if (!needsCarrier && !needsNumber) {
                continue;
            }
            String trackingNumber = needsNumber
                    ? generateCjStyleTrackingNumber()
                    : delivery.getTrackingNumber().trim();
            delivery.assignWaybill(CARRIER_CJ, trackingNumber);
            updated++;
        }
        if (updated > 0) {
            log.info("[운송장 Mock 백필] {}건을 CJ대한통운/56xxxxxxxxxx 형식으로 갱신했습니다.", updated);
        }
        return updated;
    }

    @Transactional
    public DeliveryDto.DetailResponse startShipping(
            Long orderId, DeliveryDto.StartShippingRequest request) {

        if (request == null || request.getTrackingNumber() == null
                || request.getTrackingNumber().trim().isEmpty()) {
            throw new InoutException("운송장 번호는 필수입니다.", 400, "INVALID_TRACKING_NUMBER");
        }

        LocalDateTime shippedAt = request.getShippedAt() != null
                ? request.getShippedAt() : LocalDateTime.now();

        Delivery delivery = deliveryRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new InoutException("배송 정보를 찾을 수 없습니다.", 404, "DELIVERY_NOT_FOUND"));

        if (delivery.getStatus() != DeliveryStatus.READY) {
            throw new InoutException("배송 준비 상태에서만 배송 시작이 가능합니다.", 400, "INVALID_DELIVERY_STATUS");
        }

        String carrier = StringUtils.hasText(delivery.getCarrier()) ? delivery.getCarrier() : CARRIER_CJ;
        delivery.startShipping(carrier, request.getTrackingNumber().trim(), shippedAt);
        return DeliveryDto.from(delivery);
    }

    @Transactional
    public DeliveryDto.DetailResponse completeDelivery(
            Long orderId, DeliveryDto.CompleteDeliveryRequest request) {

        LocalDateTime deliveredAt = request != null && request.getDeliveredAt() != null
                ? request.getDeliveredAt() : LocalDateTime.now();

        Delivery delivery = deliveryRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new InoutException("배송 정보를 찾을 수 없습니다.", 404, "DELIVERY_NOT_FOUND"));

        if (delivery.getStatus() != DeliveryStatus.SHIPPING) {
            throw new InoutException("배송 중 상태에서만 배송 완료 처리가 가능합니다.", 400, "INVALID_DELIVERY_STATUS");
        }

        delivery.completeDelivery(deliveredAt);
        return DeliveryDto.from(delivery);
    }
}
