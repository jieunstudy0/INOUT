package com.jstudy.inout.common.auth.entity;

import com.jstudy.inout.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(nullable = false)
    private LocalDate birthday;

    @Column(length = 30)
    private String provider;

    @Column(length = 100)
    private String providerId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
    

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<UserRole> userRoles = new ArrayList<>();

    @Column
    private boolean passwordResetYn = false;

    @Column
    private String passwordResetKey;

    @Column
    private LocalDateTime passwordResetLimit;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(nullable = false)
    @Builder.Default
    private int loginFailCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean isLocked = false;
    
    @Column
    private LocalDateTime lockedAt;

    /** 1일 예치금 사용 한도 (null = 제한 없음) */
    @Column(name = "daily_deposit_limit")
    private Long dailyDepositLimit;

    /** 오늘 사용한 예치금 누적액 */
    @Column(name = "today_used_deposit", nullable = false)
    @Builder.Default
    private Long todayUsedDeposit = 0L;
    
    public void increaseFailedAttempt() {
        this.loginFailCount++; 
        if (this.loginFailCount >= 5) {
            this.isLocked = true;
            this.lockedAt = LocalDateTime.now();
        }
    }

    public void resetLoginAttributes() {
        this.loginFailCount = 0; 
        this.isLocked = false;
        this.lockedAt = null;
    }

    public void updateInfo(String phone, Store store) {
        this.phone = phone;
        this.store = store;
    }

    public Long getId() {
        return this.id;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void setPasswordResetInfo(String resetKey) {
        this.passwordResetYn = true;
        this.passwordResetKey = resetKey;
    }

    public void clearPasswordResetInfo() {
        this.passwordResetKey = null;
        this.passwordResetYn = false;
    }
    
    
    public void updateProfile(String name, String phone, Store store) {
        this.name = name;
        this.phone = phone;
        if (store != null) {
            this.store = store;
        }
    }
    
    /**
     * 재직 상태·소속 매장 갱신.
     * RESIGNED(퇴사) 시 Soft Delete: deleted=true, store=null (지점 통계·소속에서 분리).
     * 과거 발주/문의 FK는 User 행을 유지하므로 보존된다.
     */
    public void updateStatusAndStore(UserStatus status, Store store) {
        this.status = status;

        if (status == UserStatus.RESIGNED) {
            this.deleted = true;
            this.store = null;
        } else {
            this.deleted = false;
            this.store = store;
        }
    }

    public void updateSocialProfile(String provider, String providerId) {
        this.provider = provider;
        this.providerId = providerId;
    }

    /** 1일 한도 설정. null이면 무제한. */
    public void updateDailyDepositLimit(Long dailyDepositLimit) {
        if (dailyDepositLimit != null && dailyDepositLimit < 0) {
            throw new IllegalArgumentException("1일 예치금 한도는 0 이상이어야 합니다.");
        }
        this.dailyDepositLimit = dailyDepositLimit;
    }

    /** 잔여 1일 한도 (무제한이면 Long.MAX_VALUE) */
    public long remainingDailyDepositLimit() {
        if (this.dailyDepositLimit == null) {
            return Long.MAX_VALUE;
        }
        long used = this.todayUsedDeposit != null ? this.todayUsedDeposit : 0L;
        return Math.max(0L, this.dailyDepositLimit - used);
    }

    /**
     * 결제 전 1일 한도 검증.
     * @throws IllegalStateException 한도 초과 시
     */
    public void assertCanUseDeposit(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("사용 금액은 0 이상이어야 합니다.");
        }
        if (this.dailyDepositLimit == null) {
            return;
        }
        long used = this.todayUsedDeposit != null ? this.todayUsedDeposit : 0L;
        if (used + amount > this.dailyDepositLimit) {
            throw new IllegalStateException("1일 예치금 사용 한도를 초과했습니다");
        }
    }

    /** 1일 한도 검증 후 오늘 사용액 누적 */
    public void consumeDailyDeposit(long amount) {
        assertCanUseDeposit(amount);
        long used = this.todayUsedDeposit != null ? this.todayUsedDeposit : 0L;
        this.todayUsedDeposit = used + amount;
    }

    public void resetTodayUsedDeposit() {
        this.todayUsedDeposit = 0L;
    }
    
}