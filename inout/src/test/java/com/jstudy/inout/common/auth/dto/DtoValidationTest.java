package com.jstudy.inout.common.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("UserLogin DTO - 이메일이나 비밀번호가 비어있으면 검증에 실패한다")
    void userLoginValidation() {
        // given
        UserLogin login = UserLogin.builder()
                .email("") 
                .password("   ") 
                .build();

        // when
        Set<ConstraintViolation<UserLogin>> violations = validator.validate(login);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(2);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("이메일 항목은 필수입니다.", "비밀번호 항목은 필수입니다.");
    }

    @Test
    @DisplayName("UserInput DTO - 이메일 형식이 틀리거나 비밀번호가 4자 미만이면 검증에 실패한다")
    void userInputValidation() {
        // given
        UserInput input = UserInput.builder()
                .email("wrong-email-format") 
                .password("123")
                .name("홍길동")
                .confirmPassword("123")
                .phone("010-1234-5678")
                .storeId(1L)
                .build();

        // when
        Set<ConstraintViolation<UserInput>> violations = validator.validate(input);

        // then
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains(
                        "이메일 형식에 맞게 입력해 주세요.",
                        "비밀번호는 4자 이상 입력해야 합니다.",
                        "생년월일은 필수 항목입니다."
                );
    }
}