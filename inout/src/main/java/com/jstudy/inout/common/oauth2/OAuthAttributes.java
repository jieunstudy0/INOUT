package com.jstudy.inout.common.oauth2;

import com.jstudy.inout.common.auth.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class OAuthAttributes {
  
    private final Map<String, Object> attributes;
    private final String attributeKey;
    private final String name;
    private final String email;
    private final String providerId;
    private final String provider;

    public static OAuthAttributes of(String registrationId,
                                     String userNameAttributeName,
                                     Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> ofGoogle(userNameAttributeName, attributes);
            case "kakao"  -> ofKakao(userNameAttributeName, attributes);
            case "naver"  -> ofNaver(userNameAttributeName, attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인 제공자: " + registrationId);
        };
    }


    private static OAuthAttributes ofGoogle(String userNameAttributeName,
                                            Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .providerId((String) attributes.get("sub"))
                .provider("google")
                .attributes(attributes)
                .attributeKey(userNameAttributeName)
                .build();
    }


    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofKakao(String userNameAttributeName,
                                           Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile      = (Map<String, Object>) kakaoAccount.get("profile");

        String email = (String) kakaoAccount.get("email");

        return OAuthAttributes.builder()
                .name((String) profile.get("nickname"))
                .email(email)
                .providerId(String.valueOf(attributes.get("id")))
                .provider("kakao")
                .attributes(attributes)
                .attributeKey(userNameAttributeName)   
                .build();
    }

    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofNaver(String userNameAttributeName,
                                           Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        return OAuthAttributes.builder()
                .name((String) response.get("name"))
                .email((String) response.get("email"))
                .providerId(String.valueOf(response.get("id")))
                .provider("naver")
                .attributes(response)  
                .attributeKey("id")
                .build();
    }

    public User toEntity() {
        return User.builder()
                .email(email)
                .name(name != null ? name : "소셜사용자")
                .password(UUID.randomUUID().toString())
                .phone("")
                .birthday(LocalDate.of(1970, 1, 1))
                .provider(provider)
                .providerId(providerId)
                .build();
    }
}
