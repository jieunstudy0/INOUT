package com.jstudy.inout.stock.service;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.common.util.FileStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class ImageService {

    @Value("${app.upload.path:./uploads}")
    private String uploadDir;

    public String uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InoutException("업로드할 파일이 없습니다.", 400, "FILE_EMPTY");
        }
    
        String extension = FileStorageUtil.extractAllowedExtension(
                file.getOriginalFilename(), FileStorageUtil.DEFAULT_IMAGE_EXTENSIONS);
        String savedFilename = UUID.randomUUID() + "." + extension;

        try {
            Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadRoot)) {
                Files.createDirectories(uploadRoot);
            }

            Path filePath = FileStorageUtil.resolveSafeTargetPath(uploadRoot, savedFilename);
            Files.write(filePath, file.getBytes());

            log.info("이미지 저장 완료: {}", savedFilename);
            return "/uploads/" + savedFilename;

        } catch (IOException e) {
            log.error("파일 저장 실패", e);
            throw new InoutException("이미지 저장 중 오류가 발생했습니다.", 500, "FILE_UPLOAD_ERROR");
        }
    }
}
