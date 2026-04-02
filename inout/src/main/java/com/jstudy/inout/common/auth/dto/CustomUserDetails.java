package com.jstudy.inout.common.auth.dto;

import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.entity.UserRole;
import lombok.Getter;

@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.user.getUserRoles().stream()
                .map(UserRole::getRole)
                .map(role -> {
                    String name = role.getRoleName(); 
                    String authority = name.startsWith("ROLE_") ? name : "ROLE_" + name;
                    return new SimpleGrantedAuthority(authority);
                })
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail(); 
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }
}