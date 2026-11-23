package com.jstudy.inout.stock.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.entity.BaseTimeEntity;
import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 매장 재고 실사표(영수증 한 장). 매장마다 따로 만들어진다.
 * 매일 정해진 시각(기본 9시)에 자동으로 만들어지고, 직원이 출력해 실재고를 펜으로 적는다.
 * 사진을 올리면 AI가 숫자를 읽어 채우고, 직원이 확인해 제출하면 점주가 승인할 때 매장 재고에 반영된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "stock_count_sheet")
public class StockCountSheet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sheet_id")
    private Long id;

    @Version
    private Long version;

    /** 영수증 상단에 찍히는 식별 번호. 사진 속 영수증이 이 실사표가 맞는지 확인하는 데 쓴다. */
    @Column(name = "sheet_code", nullable = false, unique = true, length = 30)
    private String sheetCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockCountStatus status;

    /** 만든 사람. 스케줄러가 자동으로 만들었으면 null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /** 마지막으로 올린 영수증 사진 경로. 본사가 AI가 읽은 숫자와 직접 비교할 때 본다. */
    @Column(name = "photo_url", length = 255)
    private String photoUrl;

    /** AI가 사진에서 읽은 실사표 번호가 이 실사표와 같은지. 못 읽었으면 null. */
    @Column(name = "sheet_code_matched")
    private Boolean sheetCodeMatched;

    private LocalDateTime scannedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;

    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private LocalDateTime reviewedAt;

    /** 가장 최근 반려 사유. 다시 제출해도 기록으로 남겨 둔다. */
    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @OneToMany(mappedBy = "sheet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<StockCountLine> lines = new ArrayList<>();

    public static StockCountSheet create(String sheetCode, Store store, User createdBy, List<StockCountLine> lines) {
        StockCountSheet sheet = new StockCountSheet();
        sheet.sheetCode = sheetCode;
        sheet.store = store;
        sheet.status = StockCountStatus.OPEN;
        sheet.createdBy = createdBy;
        for (StockCountLine line : lines) {
            line.attachTo(sheet);
            sheet.lines.add(line);
        }
        return sheet;
    }

    public List<StockCountLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    /**
     * AI가 읽은 결과를 기록한다. 실재고(countedQty)는 건드리지 않는다 — 사람이 확인해 제출해야 확정된다.
     * @param readings 줄 번호(lineNo) → 읽은 값. 없는 줄은 "읽지 못함"으로 기록한다.
     */
    public void recordAiReading(Map<Integer, AiReading> readings, Boolean sheetCodeMatched, String photoUrl) {
        requireStatus(StockCountStatus.OPEN);
        for (StockCountLine line : lines) {
            AiReading reading = readings.get(line.getLineNo());
            if (reading == null) {
                line.recordAiReading(null, false);
            } else {
                line.recordAiReading(reading.qty(), reading.confident());
            }
        }
        this.sheetCodeMatched = sheetCodeMatched;
        this.photoUrl = photoUrl;
        this.scannedAt = LocalDateTime.now();
    }

    /**
     * 직원이 확인한 실재고를 제출한다. 값이 없는(null) 줄은 이번에 세지 않은 것으로 본다.
     * 모든 값을 먼저 검사한 뒤에 기록한다.
     * @param countsByLineId 줄 ID → 실재고
     */
    public void submit(User employee, Map<Long, Integer> countsByLineId) {
        requireStatus(StockCountStatus.OPEN);
        for (Long lineId : countsByLineId.keySet()) {
            if (lines.stream().noneMatch(l -> l.getId().equals(lineId))) {
                throw new InoutException(ErrorCode.STOCK_COUNT_LINE_MISMATCH);
            }
        }
        boolean anyCounted = countsByLineId.values().stream().anyMatch(v -> v != null);
        if (!anyCounted) {
            throw new InoutException(ErrorCode.STOCK_COUNT_EMPTY);
        }
        for (StockCountLine line : lines) {
            line.submitCount(countsByLineId.get(line.getId()));
        }
        this.status = StockCountStatus.SUBMITTED;
        this.submittedBy = employee;
        this.submittedAt = LocalDateTime.now();
    }

    /** 승인 처리. 재고 반영은 서비스가 먼저 끝낸 뒤 호출한다. */
    public void markApplied(User owner) {
        requireStatus(StockCountStatus.SUBMITTED);
        this.status = StockCountStatus.APPLIED;
        this.reviewedBy = owner;
        this.reviewedAt = LocalDateTime.now();
    }

    /** 반려: 직원이 다시 세어 제출할 수 있도록 작성 중 상태로 되돌린다. */
    public void reject(User owner, String reason) {
        requireStatus(StockCountStatus.SUBMITTED);
        this.status = StockCountStatus.OPEN;
        this.reviewedBy = owner;
        this.reviewedAt = LocalDateTime.now();
        this.rejectReason = reason;
    }

    public void requireStatus(StockCountStatus expected) {
        if (this.status != expected) {
            throw new InoutException(ErrorCode.STOCK_COUNT_INVALID_STATUS);
        }
    }

    /** AI가 한 줄에서 읽은 값. qty가 null이면 칸이 비었거나 알아볼 수 없었다는 뜻이다. */
    public record AiReading(Integer qty, boolean confident) {
    }
}
