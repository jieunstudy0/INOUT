package com.jstudy.inout.stock.entity;

import java.time.LocalDateTime;
import com.jstudy.inout.common.entity.BaseTimeEntity;
import com.jstudy.inout.stock.exception.NotEnoughStockException;
import jakarta.persistence.*;
import lombok.*;

/**
 * 재고 품목(상품) 엔티티.
 *
 * [설계 원칙]
 * - 재고 증감은 addStock(int), removeStock(int) 도메인 메서드로만 처리
 * - 논리 삭제: delete() 호출 시 deleted=true, deletedAt 기록
 * - 낙관적 락: @Version 으로 동시 수정 충돌 감지
 * - 비관적 락: ItemRepository.findByIdWithLock()으로 재고 차감 시 충돌 방지
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "item")
public class Item extends BaseTimeEntity {

    /** 상품 PK. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    /**
     * 낙관적 락(Optimistic Lock) 버전 필드.
     * 동일 상품이 동시에 수정될 때 ObjectOptimisticLockingFailureException 발생.
     * GlobalExceptionHandler에서 409 Conflict로 처리됩니다.
     */
    @Version
    private Long version;

    /** 상품 카테고리. LAZY 로딩으로 불필요한 조인 방지. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ItemCategory category;

    /** 상품명. 고유값(unique) 제약. */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /** 단가 (원 단위). */
    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    /** 현재 재고 수량. 0 이하로 내려갈 수 없음 (removeStock에서 검증). */
    @Column(name = "current_stock", nullable = false)
    private Integer currentStock = 0;

    /**
     * 최소 재고 기준치.
     * currentStock <= minStockLevel 이면 '저재고' 알림 대상이 됩니다.
     */
    @Column(name = "min_stock_level", nullable = false)
    private Integer minStockLevel = 0;

    /** 단위 설명 (예: 박스, 개, kg). */
    @Column(name = "unit_description", length = 100)
    private String unitDescription;

    /** 상품 상세 설명. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 논리 삭제 플래그. true이면 삭제된 상품으로 간주. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    /** 논리 삭제 처리된 시각. deleted=true일 때 기록됨. */
    private LocalDateTime deletedAt;

    // =========================================================
    // 도메인 메서드
    // =========================================================

    /**
     * 재고를 증가시킵니다 (입고 처리).
     * StockAdmService.receiveStock()과 adjustStock()에서 호출됩니다.
     *
     * @param quantity 증가시킬 수량 (양수)
     */
    public void addStock(int quantity) {
        this.currentStock += quantity;
    }

    /**
     * 재고를 감소시킵니다 (사용/출고 처리).
     * 재고 부족 시 NotEnoughStockException 을 발생시킵니다.
     * StockEmpService.useStock(), OrderAdmService.approveItemStock()에서 호출됩니다.
     *
     * @param quantity 감소시킬 수량 (양수)
     * @throws NotEnoughStockException 잔여 재고가 요청 수량보다 적을 때
     */
    public void removeStock(int quantity) {
        int restStock = this.currentStock - quantity;
        if (restStock < 0) {
            throw NotEnoughStockException.withCurrentStock(this.currentStock, quantity);
        }
        this.currentStock = restStock;
    }

    /**
     * 상품 기본 정보를 업데이트합니다.
     * JPA Dirty Checking에 의해 별도 save() 없이 트랜잭션 종료 시 DB에 반영됩니다.
     *
     * @param name            새 상품명
     * @param category        새 카테고리
     * @param unitPrice       새 단가
     * @param minStockLevel   새 최소 재고 기준치
     * @param unitDescription 새 단위 설명
     * @param description     새 상세 설명
     */
    public void updateInfo(String name, ItemCategory category, Long unitPrice,
            Integer minStockLevel, String unitDescription, String description) {
        this.name = name;
        this.category = category;
        this.unitPrice = unitPrice;
        this.minStockLevel = minStockLevel;
        this.unitDescription = unitDescription;
        this.description = description;
    }

    /**
     * 상품을 논리 삭제 처리합니다.
     * 물리 삭제 없이 deleted=true로 표시하고 삭제 시각을 기록합니다.
     * StockAdmService.deleteStock()에서 호출됩니다.
     */
    public void delete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}