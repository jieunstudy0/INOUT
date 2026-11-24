package com.jstudy.inout.stock.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.stock.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 점주용 사진 업로드 (직원 사진). 본사는 /api/admin/images를 쓴다. 받은 경로를 직원 기본 정보 수정에 넣는다. */
@Tag(name = "사진 업로드 (점주)", description = "jpg·png·gif·webp, 10MB 이하")
@RestController
@RequestMapping("/api/owner/images")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class OwnerImageController {

    private final ImageService imageService;

    @Operation(summary = "사진 업로드", description = "저장된 사진 경로(/uploads/...)를 돌려줍니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        return ResponseResult.success("사진이 업로드되었습니다.", imageService.uploadImage(file));
    }
}
