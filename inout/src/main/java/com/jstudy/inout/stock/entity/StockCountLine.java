package com.jstudy.inout.stock.entity;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 실사표의 한 줄 = 매장 재고 한 행. 출력 시점의 상품 정보와 매장 재고 수량을 그대로 복사해 둔다(스냅샷).
 * 출력 후 상품 이름·위치·재고가 바뀌어도 영수증에 찍힌 내용과 같아야 사진과 맞춰 볼 수 있기 때문이다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "stock_count_line",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sheet_id", "line_no"}))
public class StockCountLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id", nullable = false)
    private StockCountSheet sheet;

    /** 영수증 맨 앞에 찍히는 번호(1부터). AI가 사진의 줄과 상품을 맞추는 기준이다. */
    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_stock_id", nullable = false)
    private StoreStock storeStock;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(length = 20)
    private String location;

    @Column(name = "unit_description", length = 100)
    private String unitDescription;

    /** 출력 시점의 매장 재고. 반영할 차이는 (실재고 - 이 값)으로 계산한다. */
    @Column(name = "system_qty", nullable = false)
    private Integer systemQty;

    @Column(name = "min_stock_level", nullable = false)
    private Integer minStockLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockCountReason reason;

    /** AI가 사진에서 읽은 수량. 읽지 못했으면 null. 참고용이며 그대로 반영하지 않는다. */
    @Column(name = "ai_counted_qty")
    private Integer aiCountedQty;

    /** AI가 글씨를 확실히 읽었는지. false면 화면에서 강조해 사람이 다시 보게 한다. */
    @Column(name = "ai_confident")
    private Boolean aiConfident;

    /** 직원이 확인·제출한 실재고. null이면 이번에 세지 않은 상품으로 보고 반영하지 않는다. */
    @Column(name = "counted_qty")
    private Integer countedQty;

    /** 승인 시 실제로 더하거나 뺀 수량. 반영 전이면 null, 차이가 없었으면 0. */
    @Column(name = "applied_diff")
    private Integer appliedDiff;

    @Builder
    private StockCountLine(Integer lineNo, StoreStock storeStock, StockCountReason reason) {
        Item item = storeStock.getItem();
        this.lineNo = lineNo;
        this.storeStock = storeStock;
        this.itemId = item.getItemId();
        this.itemName = item.getName();
        this.location = storeStock.getLocation();
        this.unitDescription = item.getUnitDescription();
        this.systemQty = storeStock.getQuantity();
        this.minStockLevel = storeStock.getMinStockLevel();
        this.reason = reason;
    }

    void attachTo(StockCountSheet sheet) {
        this.sheet = sheet;
    }

    void recordAiReading(Integer qty, Boolean confident) {
        this.aiCountedQty = qty;
        this.aiConfident = confident;
    }

    void submitCount(Integer qty) {
        this.countedQty = qty;
    }

    public void markApplied(int diff) {
        this.appliedDiff = diff;
    }

    /** 실재고 - 출력 시점 시스템 수량. 세지 않은 줄은 null. */
    public Integer getDiff() {
        return countedQty == null ? null : countedQty - systemQty;
    }
}
