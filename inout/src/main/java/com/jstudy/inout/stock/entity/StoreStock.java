package com.jstudy.inout.stock.entity;

import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.entity.BaseTimeEntity;
import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 매장 재고 — 매장 한 곳이 가진 상품 한 가지의 수량.
 *
 * 본사 창고 재고(Item.currentStock)와는 별개다.
 * - 본사 재고: 발주 승인 시 줄고, 본사 입고·조정으로 늘어난다 (기존 그대로).
 * - 매장 재고: 매장이 배송을 수령 확인하면 늘고, 직원 사용·점주 조정·실사 승인으로 바뀐다.
 * 행은 처음 수령하거나 점주가 처음 등록할 때 만들어진다 (매장이 다루지 않는 상품은 행이 없다).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "store_stock",
        uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "item_id"}))
public class StoreStock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_stock_id")
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private Integer quantity;

    /** 매장 안전재고. 이 값 이하이면 실사표·저재고 목록에 오른다. 처음에는 상품의 기본 안전재고를 복사한다. */
    @Column(name = "min_stock_level", nullable = false)
    private Integer minStockLevel;

    /** 매장 안 보관 위치 (예: C-01). 실사 영수증의 정렬 순서가 된다. */
    @Column(length = 20)
    private String location;

    public static StoreStock create(Store store, Item item) {
        StoreStock stock = new StoreStock();
        stock.store = store;
        stock.item = item;
        stock.quantity = 0;
        stock.minStockLevel = item.getMinStockLevel() != null ? item.getMinStockLevel() : 0;
        return stock;
    }

    public void increase(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("증가 수량은 1 이상이어야 합니다. amount=" + amount);
        }
        this.quantity += amount;
    }

    public void decrease(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("차감 수량은 1 이상이어야 합니다. amount=" + amount);
        }
        if (this.quantity < amount) {
            throw new InoutException(ErrorCode.STORE_STOCK_SHORTAGE, String.format(
                    "매장 재고가 부족합니다. (현재 %d개, 요청 %d개)", this.quantity, amount));
        }
        this.quantity -= amount;
    }

    public void changeSettings(Integer minStockLevel, String location) {
        this.minStockLevel = minStockLevel;
        this.location = location;
    }

    public boolean isLow() {
        return quantity <= minStockLevel;
    }
}
