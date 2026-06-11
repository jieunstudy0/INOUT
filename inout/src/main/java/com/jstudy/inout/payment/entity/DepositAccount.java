package com.jstudy.inout.payment.entity;

import com.jstudy.inout.common.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepositAccount {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Long balance = 0L;

    @Version 
    private Long version;

    @Builder
    public DepositAccount(User user, Long balance) {
        this.user = user;
        this.balance = balance != null ? balance : 0L;
    }

    public void addBalance(Long amount) {
        if (amount <= 0) throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        this.balance += amount;
    }

    public void deductBalance(Long amount) {
        if (this.balance < amount) throw new IllegalStateException("잔액이 부족합니다.");
        this.balance -= amount;
    }
}