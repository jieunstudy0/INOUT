package com.jstudy.inout.inquiry.dto;

import com.jstudy.inout.inquiry.entity.InquiryTargetType;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class InquiryCreateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이내로 작성해주세요.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @NotNull(message = "수신 대상을 선택해주세요.")
    private InquiryTargetType targetType = InquiryTargetType.ADMIN;

    private MultipartFile file;
}