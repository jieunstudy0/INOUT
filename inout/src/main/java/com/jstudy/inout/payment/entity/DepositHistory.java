package com.jstudy.inout.payment.entity;

import com.jstudy.inout.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) 
public class DepositHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_account_id", nullable = false)
    private DepositAccount depositAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private String description;

    @Column(name = "related_order_id", nullable = true)
    private Long relatedOrderId;

    @Column(name = "processed_by", nullable = false)
    private Long processedBy;

    @Column(name = "admin_memo", length = 500)
    private String adminMemo;

    @Column(name = "balance_after")
    private Long balanceAfter;

    @Builder
    public DepositHistory(DepositAccount depositAccount, TransactionType type, 
                          Long amount, String description, 
                          Long relatedOrderId, Long processedBy, String adminMemo, Long balanceAfter) {
        this.depositAccount = depositAccount;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.relatedOrderId = relatedOrderId;
        this.processedBy = processedBy;
        this.adminMemo = adminMemo;
        this.balanceAfter = balanceAfter;
    }

    public void updateAdminMemo(String adminMemo) {
        this.adminMemo = adminMemo;
    }
}