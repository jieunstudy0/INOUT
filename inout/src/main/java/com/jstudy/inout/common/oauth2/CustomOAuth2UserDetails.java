package com.jstudy.inout.common.oauth2;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.entity.UserRole;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class CustomOAuth2UserDetails implements OAuth2User {

    private final User user;
    private final Map<String, Object> attributes;

    private final String attributeKey;

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getUserRoles().stream()
                .map(UserRole::getRole)
                .map(role -> {
                    String name = role.getRoleName();
                    return new SimpleGrantedAuthority(name.startsWith("ROLE_") ? name : "ROLE_" + name);
                })
                .collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return String.valueOf(attributes.get(attributeKey));
    }
}
