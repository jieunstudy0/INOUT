package com.jstudy.inout.request.entity;

import java.time.LocalDateTime;
import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.entity.BaseTimeEntity;
import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 현장 수정 요청. 현장에서 문제를 발견한 사람이 그 자리에서 남기고, 권한 있는 사람이 처리한다.
 *
 * 요청 자체는 데이터를 바꾸지 않는다. 처리하는 사람이 기존 기능(점주 재고 조정, 발주 수정 등)으로 고친 뒤
 * 이 요청을 완료로 바꾼다. 그래야 "재고·돈은 검사 후 반영" 규칙과 이력이 그대로 지켜진다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "change_request", indexes = {
        @Index(name = "idx_change_request_store_status", columnList = "store_id, status"),
        @Index(name = "idx_change_request_requester", columnList = "requester_id")
})
public class ChangeRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    /** 요청한 사람이 점주였는지 (결과 알림을 어느 화면으로 보낼지 정한다) */
    @Column(name = "requested_by_owner", nullable = false)
    private boolean requestedByOwner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChangeRequestCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChangeRequestCategory.Handler handler;

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", nullable = false, length = 20)
    private ChangeRequestRefType refType;

    @Column(name = "ref_id")
    private Long refId;

    /** 대상 이름 스냅샷 (예: "발주 #12", "원두 1kg") */
    @Column(name = "ref_label", length = 100)
    private String refLabel;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "photo_url", length = 255)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChangeRequestStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private User handledBy;

    private LocalDateTime handledAt;

    /** 처리한 사람이 남긴 답변 (반려 사유 포함) */
    @Column(name = "response_message", length = 255)
    private String responseMessage;

    @Builder
    private ChangeRequest(Store store, User requester, boolean requestedByOwner, ChangeRequestCategory category,
                          ChangeRequestRefType refType, Long refId, String refLabel,
                          String title, String content, String photoUrl) {
        this.store = store;
        this.requester = requester;
        this.requestedByOwner = requestedByOwner;
        this.category = category;
        this.handler = category.getHandler();
        this.refType = refType == null ? ChangeRequestRefType.NONE : refType;
        this.refId = refId;
        this.refLabel = refLabel;
        this.title = title;
        this.content = content;
        this.photoUrl = photoUrl;
        this.status = ChangeRequestStatus.REQUESTED;
    }

    /**
     * 상태 변경. 끝난 요청(완료·반려)은 다시 바꿀 수 없고, 접수 상태로 되돌릴 수도 없다.
     * 반려할 때는 사유가 필요하다.
     */
    public void changeStatus(User handler, ChangeRequestStatus next, String message) {
        if (status.isFinished()) {
            throw new InoutException(ErrorCode.ALREADY_PROCESSED, "이미 처리가 끝난 요청입니다.");
        }
        if (next == ChangeRequestStatus.REQUESTED || next == status) {
            throw new InoutException(ErrorCode.INVALID_REQUEST_STATUS);
        }
        if (next == ChangeRequestStatus.REJECTED && (message == null || message.isBlank())) {
            throw new InoutException(ErrorCode.REJECT_REASON_REQUIRED);
        }
        this.status = next;
        this.handledBy = handler;
        this.handledAt = LocalDateTime.now();
        if (message != null && !message.isBlank()) {
            this.responseMessage = message.trim();
        }
    }
}
