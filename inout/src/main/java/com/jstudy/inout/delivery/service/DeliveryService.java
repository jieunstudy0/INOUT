package com.jstudy.inout.delivery.service;

import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.delivery.dto.DeliveryDto;
import com.jstudy.inout.delivery.entity.Delivery;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import com.jstudy.inout.delivery.repository.DeliveryRepository;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    @Transactional
    public void createDeliveryIfAbsentForCompletedOrder(OrderRequest order) {
        if (order == null || order.getId() == null) {
            return;
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
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

    public DeliveryDto.DetailResponse getDeliveryByOrderId(Long orderId) {
        Delivery delivery = deliveryRepository.findByOrderRequest_Id(orderId)
                .orElseThrow(() -> new InoutException("배송 정보를 찾을 수 없습니다.", 404, "DELIVERY_NOT_FOUND"));
        return DeliveryDto.from(delivery);
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

        delivery.startShipping(request.getTrackingNumber().trim(), shippedAt);
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
