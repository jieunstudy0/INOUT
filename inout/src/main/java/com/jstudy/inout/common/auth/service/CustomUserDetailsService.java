package com.jstudy.inout.common.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.auth.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository; 

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        return userRepository.findByEmailActive(email) 
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException(email + " 사용자를 찾을 수 없거나 탈퇴한 회원입니다."));
    }
}