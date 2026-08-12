package com.jstudy.inout.common.oauth2;

import com.jstudy.inout.common.auth.entity.Role;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.entity.UserRole;
import com.jstudy.inout.common.auth.repository.RoleRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.auth.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId        = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        OAuthAttributes attrs = OAuthAttributes.of(
                registrationId, userNameAttributeName, oAuth2User.getAttributes());

        if (attrs.getEmail() == null || attrs.getEmail().isBlank()) {
            log.warn("OAuth2 로그인 실패: {} 계정에서 이메일을 제공하지 않았습니다.", registrationId);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_provided"),
                    "이메일 정보를 제공받지 못했습니다. 소셜 계정의 이메일 공개 설정을 확인해 주세요.");
        }

        User user = saveOrUpdate(attrs);

        User userWithRoles = userRepository.findByEmail(user.getEmail()).orElse(user);

        log.info("OAuth2 사용자 처리 완료: email={}, provider={}", user.getEmail(), registrationId);
        return new CustomOAuth2UserDetails(userWithRoles, attrs.getAttributes(), attrs.getAttributeKey());
    }

    private User saveOrUpdate(OAuthAttributes attrs) {
        return userRepository.findByEmail(attrs.getEmail())
                .map(existing -> {
                    existing.updateSocialProfile(attrs.getProvider(), attrs.getProviderId());
                    return existing;
                })
                .orElseGet(() -> createSocialUser(attrs));
    }

    private User createSocialUser(OAuthAttributes attrs) {
        User newUser = userRepository.save(attrs.toEntity());

        Role defaultRole = roleRepository.findByRoleName("ROLE_EMPLOYEE")
                .orElseThrow(() -> new IllegalStateException(
                        "기본 권한 ROLE_EMPLOYEE가 존재하지 않습니다. 초기 데이터를 확인해 주세요."));

        userRoleRepository.save(UserRole.builder()
                .user(newUser)
                .role(defaultRole)
                .build());

        log.info("신규 소셜 사용자 등록: email={}, provider={}", newUser.getEmail(), newUser.getProvider());
        return newUser;
    }
}
