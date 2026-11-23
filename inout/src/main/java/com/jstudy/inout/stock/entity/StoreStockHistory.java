package com.jstudy.inout.stock.entity;

import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.jstudy.inout.common.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 매장 재고 변경 이력. 모든 증감은 이 표에 한 줄씩 남는다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "store_stock_history",
        indexes = @Index(name = "idx_store_stock_history_stock_date", columnList = "store_stock_id, created_at"))
public class StoreStockHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_stock_id", nullable = false)
    private StoreStock storeStock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreStockChangeType type;

    /** 증감량. 늘면 양수, 줄면 음수. */
    @Column(name = "change_qty", nullable = false)
    private Integer changeQty;

    /** 변경 후 수량 */
    @Column(name = "result_qty", nullable = false)
    private Integer resultQty;

    @Column(length = 255)
    private String memo;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private StoreStockHistory(StoreStock storeStock, User user, StoreStockChangeType type,
                              Integer changeQty, String memo) {
        this.storeStock = storeStock;
        this.user = user;
        this.type = type;
        this.changeQty = changeQty;
        this.resultQty = storeStock.getQuantity();
        this.memo = memo;
    }
}
