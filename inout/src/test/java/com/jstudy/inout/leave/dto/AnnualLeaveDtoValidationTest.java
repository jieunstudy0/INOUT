package com.jstudy.inout.leave.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnnualLeaveDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("CreateRequest - 시작일/종료일/종류 누락 시 검증 실패")
    void createRequest_fail_required() {
        // given
        AnnualLeaveDto.CreateRequest request = AnnualLeaveDto.CreateRequest.builder().build();

        // when
        Set<ConstraintViolation<AnnualLeaveDto.CreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("ProcessRequest - 처리 상태 누락 시 검증 실패")
    void processRequest_fail_statusRequired() {
        // given
        AnnualLeaveDto.ProcessRequest request = AnnualLeaveDto.ProcessRequest.builder().build();

        // when
        Set<ConstraintViolation<AnnualLeaveDto.ProcessRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("처리 상태를 선택해주세요.");
    }
}
