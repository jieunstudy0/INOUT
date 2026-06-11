package com.jstudy.inout.inquiry.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InquiryDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("CommentCreateRequest 검증 - 댓글 내용이 비어있으면 에러가 발생한다")
    void commentCreateRequest_Validation_Fail() {
        // given
        CommentCreateRequest request = CommentCreateRequest.builder()
                .content("") 
                .parentId(null)
                .build();

        // when
        Set<ConstraintViolation<CommentCreateRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("댓글 내용은 필수입니다.");
    }
}