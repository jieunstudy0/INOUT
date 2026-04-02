package com.jstudy.inout.common.auth.entity;

import com.jstudy.inout.common.entity.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
    private boolean locked = false;  
 
    public void increaseLoginFailCount() {
        this.loginFailCount++;
        if (this.loginFailCount >= 5) {
            this.locked = true; 
        }
    }

    public void resetLoginFailCount() {
        this.loginFailCount = 0;  
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

}