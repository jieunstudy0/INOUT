package com.jstudy.inout.common.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserProfileUpdateRequest {
    @NotBlank(message = "이름은 필수 입력입니다.")
    private String name;
    
    private String storeName; 
    
    @NotBlank(message = "핸드폰 번호는 필수 입력입니다.")
    private String phone;
    
    private String password;
}