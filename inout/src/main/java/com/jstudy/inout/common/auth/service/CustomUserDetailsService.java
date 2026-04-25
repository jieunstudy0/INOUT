package com.jstudy.inout.common.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.LockedException; // 💡 LockedException 임포트 추가!
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository; 

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

         User user = userRepository.findByEmailActive(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 이메일로 가입된 사용자를 찾을 수 없습니다: " + email));

        if (user.isLocked()) {
            throw new LockedException("계정이 잠겼습니다. 관리자에게 문의해주세요.");
        }

        return new CustomUserDetails(user);
    }
}