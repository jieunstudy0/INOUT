package com.jstudy.inout.common.auth.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@IdClass(UserRoleId.class) 
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Getter 
@ToString
@EqualsAndHashCode
public class UserRole {

  
    @Id 
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "user_id")
    private User user;

    @Id 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id") 
    private Role role; 
}