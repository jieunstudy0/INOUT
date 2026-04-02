package com.jstudy.inout.common.auth.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInput {

    @Email(message = "이메일 형식에 맞게 입력해 주세요.")
    @NotBlank(message = "이메일은 필수 항목입니다.")
    private String email;

    @NotBlank(message = "이름은 필수 항목입니다.")
    private String name;

    @Size(min = 4, message = "비밀번호는 4자 이상 입력해야 합니다.")
    @NotBlank(message = "비밀번호는 필수 항목입니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수 항목입니다.")
    private String confirmPassword;

    @Size(max = 20, message = "연락처는 최대 20자까지 입력해야 합니다.")
    @NotBlank(message = "연락처는 필수 항목입니다.")
    private String phone;

    @NotBlank(message = "지점 이름은 필수 항목입니다.")
    private Long storeId;

    @NotBlank(message = "생년월일은 필수 항목입니다.")
    private LocalDate birthday; 
}

