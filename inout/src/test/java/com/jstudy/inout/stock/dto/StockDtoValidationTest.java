package com.jstudy.inout.stock.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.jstudy.inout.stock.dto.admin.StockReceiveRequest;
import com.jstudy.inout.stock.dto.admin.StockRegister;
import com.jstudy.inout.stock.dto.admin.StockUpdate;

class StockDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("StockRegister 검증 - 단가가 음수이거나 상품명이 비어있으면 에러가 발생한다")
    void stockRegisterValidation_Fail() {
        // given
        StockRegister request = StockRegister.builder()
                .name("") 
                .categoryId(1)
                .unitPrice(-500L) 
                .minStockLevel(10)
                .build();

        // when
        Set<ConstraintViolation<StockRegister>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(2);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "상품명은 필수입니다.", 
                        "단가는 0원 이상이어야 합니다."
                );
    }

    @Test
    @DisplayName("StockReceiveRequest 검증 - 입고 수량이 1 미만이면 에러가 발생한다")
    void stockReceiveRequestValidation_Fail() {
        // given
        StockReceiveRequest request = StockReceiveRequest.builder()
                .itemId(1L)
                .quantity(0) 
                .memo("테스트")
                .build();

        // when
        Set<ConstraintViolation<StockReceiveRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("입고 수량은 1개 이상이어야 합니다.");
    }

    @Test
    @DisplayName("StockUpdate 검증 - 모든 필수 값이 정상일 때 에러가 발생하지 않는다")
    void stockUpdateValidation_Success() {
        // given
        StockUpdate request = StockUpdate.builder()
                .name("수정상품명")
                .categoryId(2)
                .unitPrice(2000L)
                .minStockLevel(5)
                .build();

        // when
        Set<ConstraintViolation<StockUpdate>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }
}