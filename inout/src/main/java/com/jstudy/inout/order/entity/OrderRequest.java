package com.jstudy.inout.order.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import com.jstudy.inout.common.auth.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "order_request") 
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OrderRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_user_id", nullable = false)
    private User requestUser; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_user_id")
    private User processUser;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Long totalPrice;
    
    private LocalDateTime requestDate;
    
    private LocalDateTime processDate; 
    
    private String rejectReason;
    
    @OneToMany(mappedBy = "orderRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default 
    private List<OrderDetail> orderDetails = new ArrayList<>();

    public void updateStatus(OrderStatus newStatus) {
        this.status = newStatus;
    }

    public void updateProcessDate(LocalDateTime date) {
        this.processDate = date;
    }
      
    public void updateRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
    
}