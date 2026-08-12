package com.jstudy.inout.common.auth.service;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.entity.UserStatus;
import com.jstudy.inout.common.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "해당 이메일로 가입된 사용자를 찾을 수 없습니다: " + email));

        if (user.isLocked()) {
            throw new LockedException("계정이 잠겼습니다. 관리자에게 문의해주세요.");
        }

        UserStatus status = user.getStatus() != null ? user.getStatus() : UserStatus.ACTIVE;
        if (!status.allowsLogin() || user.isDeleted()) {
            String message = switch (status) {
                case ON_LEAVE -> "휴직 상태의 계정입니다. 점주·관리자에게 문의해 주세요.";
                case RESIGNED -> "퇴사 처리된 계정입니다. 로그인이 불가합니다.";
                default -> "비활성 계정입니다. 로그인이 불가합니다.";
            };
            throw new DisabledException(message);
        }

        return new CustomUserDetails(user);
    }
}
