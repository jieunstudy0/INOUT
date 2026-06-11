package com.jstudy.inout.delivery.entity;

import com.jstudy.inout.common.entity.BaseTimeEntity;
import com.jstudy.inout.order.entity.OrderRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "delivery")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_id")
    private Long id;

    @Version
    private Long version;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private OrderRequest orderRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status;

    @Column(nullable = false, length = 100)
    private String receiverName;

    @Column(nullable = false, length = 30)
    private String receiverPhone;

    @Column(nullable = false, length = 255)
    private String destinationAddress;

    @Column(length = 100)
    private String trackingNumber;

    private LocalDateTime shippedAt;

    private LocalDateTime deliveredAt;

    @Builder
    public Delivery(
            OrderRequest orderRequest,
            DeliveryStatus status,
            String receiverName,
            String receiverPhone,
            String destinationAddress,
            String trackingNumber,
            LocalDateTime shippedAt,
            LocalDateTime deliveredAt) {
        this.orderRequest = orderRequest;
        this.status = status != null ? status : DeliveryStatus.READY;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.destinationAddress = destinationAddress;
        this.trackingNumber = trackingNumber;
        this.shippedAt = shippedAt;
        this.deliveredAt = deliveredAt;
    }

    public void startShipping(String trackingNumber, LocalDateTime shippedAt) {
        this.trackingNumber = trackingNumber;
        this.shippedAt = shippedAt;
        this.status = DeliveryStatus.SHIPPING;
    }

    public void completeDelivery(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
        this.status = DeliveryStatus.COMPLETED;
    }
}
