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

/**
 * 시스템 사용자(직원/관리자) 엔티티.
 *
 * 이 엔티티는 로그인, 권한 관리, 비밀번호 초기화 등
 * 사용자와 관련된 모든 핵심 정보를 담습니다.
 *
 * [설계 원칙]
 * - @Setter 를 사용하지 않고 도메인 메서드(changePassword 등)로만 상태 변경
 * - 논리 삭제(soft delete) 방식 적용 — deleted 플래그로 삭제 처리
 * - 생성/수정 시각은 BaseTimeEntity 가 자동 관리
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class User extends BaseTimeEntity {

    /** 사용자 PK. DB 컬럼명은 user_id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    /**
     * 로그인 이메일 (= 로그인 ID).
     * unique 제약으로 중복 가입 방지.
     */
    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt로 암호화된 비밀번호. 평문을 절대 저장하지 않음. */
    @Column(nullable = false)
    private String password;

    /** 사용자 실명. */
    @Column(nullable = false)
    private String name;

    /** 연락처 (최대 20자). */
    @Column(nullable = false)
    private String phone;

    /**
     * 소속 매장.
     * LAZY 로딩으로 불필요한 조인 방지.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    /** 생년월일. */
    @Column(nullable = false)
    private LocalDate birthday;

    /**
     * 사용자 권한 목록 (다대다 중간 테이블 UserRole 활용).
     * CascadeType.ALL: User 삭제 시 UserRole도 함께 삭제.
     */
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<UserRole> userRoles = new ArrayList<>();

    /**
     * 비밀번호 초기화 요청 여부.
     * true이면 초기화 링크가 발송된 상태.
     */
    @Column
    private boolean passwordResetYn = false;

    /** 비밀번호 초기화 링크에 포함되는 UUID 키. 사용 완료 시 null로 초기화. */
    @Column
    private String passwordResetKey;

    /**
     * 비밀번호 초기화 링크 만료 시각.
     * (현재는 updatedAt + 30분으로 계산하므로 직접 사용되진 않지만 확장용으로 보존)
     */
    @Column
    private LocalDateTime passwordResetLimit;

    /**
     * 논리 삭제 플래그.
     * true이면 탈퇴한 사용자로 간주 — DB에서 실제로 삭제하지 않음.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    /**
     * 로그인 연속 실패 횟수.
     * 5회 이상 실패 시 계정이 자동으로 잠김.
     */
    @Column(nullable = false)
    @Builder.Default
    private int loginFailCount = 0;

    /**
     * 계정 잠금 여부.
     * true이면 로그인 불가. 관리자가 직접 해제해야 함.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean locked = false;

    // =========================================================
    // 도메인 메서드 (비즈니스 로직 캡슐화)
    // @Setter 대신 아래 메서드들을 통해서만 상태를 변경합니다.
    // =========================================================

    /**
     * 로그인 실패 횟수를 1 증가시키고, 5회 이상이면 계정을 잠급니다.
     * AuthLoginController의 catch 블록에서 호출됩니다.
     */
    public void increaseLoginFailCount() {
        this.loginFailCount++;
        if (this.loginFailCount >= 5) {
            this.locked = true;
        }
    }

    /**
     * 로그인 성공 시 실패 횟수를 0으로 초기화합니다.
     */
    public void resetLoginFailCount() {
        this.loginFailCount = 0;
    }

    /**
     * 전화번호와 소속 매장 정보를 업데이트합니다.
     * AuthServiceImpl.updateUser()에서 Dirty Checking을 통해 자동 반영됩니다.
     *
     * @param phone 새 전화번호
     * @param store 새 소속 매장
     */
    public void updateInfo(String phone, Store store) {
        this.phone = phone;
        this.store = store;
    }

    /** ID 조회용 명시적 메서드. Lombok @Getter와 중복이지만 가독성을 위해 유지. */
    public Long getId() {
        return this.id;
    }

    /**
     * 비밀번호를 BCrypt 암호화된 새 비밀번호로 교체합니다.
     * 반드시 passwordEncoder.encode() 결과를 전달해야 합니다.
     *
     * @param encodedPassword 암호화된 새 비밀번호
     */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /**
     * 비밀번호 초기화 요청 시 UUID 키를 설정하고 초기화 요청 플래그를 true로 변경합니다.
     * AuthServiceImpl.resetPassword()에서 호출됩니다.
     *
     * @param resetKey 이메일로 발송될 UUID 키
     */
    public void setPasswordResetInfo(String resetKey) {
        this.passwordResetYn = true;
        this.passwordResetKey = resetKey;
    }

    /**
     * 비밀번호 초기화 완료 또는 링크 만료 시 초기화 관련 정보를 모두 지웁니다.
     * AuthServiceImpl.completePasswordReset()과 UserController.checkResetKey()에서 호출됩니다.
     */
    public void clearPasswordResetInfo() {
        this.passwordResetKey = null;
        this.passwordResetYn = false;
    }
}