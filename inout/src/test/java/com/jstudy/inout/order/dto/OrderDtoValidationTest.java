package com.jstudy.inout.order.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("장바구니 추가 DTO 검증 - 상품 ID 누락, 수량 0개 이하일 때 에러 발생")
    void cartAddRequest_Validation_Fail() {
        // given
        CartAddRequest request = new CartAddRequest(null, 0);

        // when
        Set<ConstraintViolation<CartAddRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(2);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "상품 ID는 필수입니다.",
                        "주문 수량은 최소 1개 이상이어야 합니다."
                );
    }

    @Test
    @DisplayName("발주 생성 DTO 검증 - 선택된 장바구니 ID 목록이 비어있으면 에러 발생")
    void orderCreateRequest_Validation_Fail_EmptyList() {
        // given
        OrderCreateRequest request = OrderCreateRequest.builder()
                .cartDetailIds(List.of()) 
                .memo("빠른 배송 부탁드립니다.")
                .build();

        // when
        Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("발주할 상품을 선택해주세요.");
    }
    
    @Test
    @DisplayName("발주 생성 DTO 검증 - 정상 데이터 통과")
    void orderCreateRequest_Validation_Success() {
        // given
        OrderCreateRequest request = OrderCreateRequest.builder()
                .cartDetailIds(List.of(10L, 11L)) 
                .build();

        // when
        Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty(); 
    }

    @Test
    @DisplayName("발주 생성 DTO 검증 - 선택 배송 필드가 길이 제한을 넘으면 에러 발생")
    void orderCreateRequest_Validation_Fail_receiverNameTooLong() {
        String tooLong = "x".repeat(101);
        OrderCreateRequest request = OrderCreateRequest.builder()
                .cartDetailIds(List.of(10L))
                .receiverName(tooLong)
                .build();

        Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations).extracting(ConstraintViolation::getPropertyPath).map(Object::toString)
                .contains("receiverName");
    }
}