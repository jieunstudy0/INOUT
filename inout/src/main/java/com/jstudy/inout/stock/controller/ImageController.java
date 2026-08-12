package com.jstudy.inout.stock.controller;

import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.stock.service.ImageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "이미지 관리", description = "상품 이미지 업로드 API")
@RestController
@RequestMapping("/api/admin/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @Operation(summary = "이미지 업로드", description = "상품 이미지를 서버에 저장하고 접속 가능한 URL을 반환합니다.")
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        String imageUrl = imageService.uploadImage(file);

        return ResponseResult.success("이미지가 성공적으로 업로드되었습니다.", imageUrl);
    }
}