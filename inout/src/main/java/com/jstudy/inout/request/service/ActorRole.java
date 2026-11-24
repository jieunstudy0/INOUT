package com.jstudy.inout.request.service;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

/** 요청을 다루는 사람의 역할. 로그인 정보(권한 목록)에서 정한다. */
public enum ActorRole {
    ADMIN, OWNER, EMPLOYEE;

    public static ActorRole from(Collection<? extends GrantedAuthority> authorities) {
        boolean admin = false;
        boolean owner = false;
        for (GrantedAuthority a : authorities) {
            String name = a.getAuthority();
            if ("ROLE_ADMIN".equals(name)) admin = true;
            if ("ROLE_OWNER".equals(name)) owner = true;
        }
        if (admin) return ADMIN;
        if (owner) return OWNER;
        return EMPLOYEE;
    }
}
