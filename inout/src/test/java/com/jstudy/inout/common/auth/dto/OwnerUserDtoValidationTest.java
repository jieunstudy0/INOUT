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

class OwnerUserDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("OwnerUserDto.CreateRequest - 필수값 누락 시 Bean Validation 실패")
    void createRequest_fail_requiredFields() {
        // given
        OwnerUserDto.CreateRequest request = OwnerUserDto.CreateRequest.builder()
                .email("")
                .name("")
                .password("12")
                .confirmPassword("")
                .phone("")
                .build();

        // when
        Set<ConstraintViolation<OwnerUserDto.CreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .anyMatch(msg -> msg.contains("이메일") || msg.contains("이름") || msg.contains("비밀번호") || msg.contains("생년월일"));
    }

    @Test
    @DisplayName("OwnerUserDto.CreateRequest - 잘못된 이메일이면 검증 실패")
    void createRequest_fail_invalidEmail() {
        // given
        OwnerUserDto.CreateRequest request = OwnerUserDto.CreateRequest.builder()
                .email("not-an-email")
                .name("직원")
                .password("inout1234!")
                .confirmPassword("inout1234!")
                .phone("010-1111-2222")
                .birthday(java.time.LocalDate.of(1995, 1, 1))
                .build();

        // when
        Set<ConstraintViolation<OwnerUserDto.CreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("이메일 형식에 맞게 입력해 주세요.");
    }
}
