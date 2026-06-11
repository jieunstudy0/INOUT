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
    
    public void updateStatusAndStore(UserStatus status, Store store) {
        this.status = status;
        this.store = store;

        if (status == UserStatus.RESIGNED) {
            this.deleted = true;
        } else {
            this.deleted = false;
        }
    }
    
}