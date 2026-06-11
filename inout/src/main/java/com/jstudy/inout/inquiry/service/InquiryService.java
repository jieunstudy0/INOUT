package com.jstudy.inout.inquiry.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.inquiry.dto.*;
import com.jstudy.inout.inquiry.entity.Inquiry;
import com.jstudy.inout.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;


@Slf4j 
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    @Value("${file.upload.dir:./uploads/inquiries/}")
    private String uploadDir;

    @Transactional
    public Long createInquiry(Long userId, InquiryCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404, "USER_NOT_FOUND"));

        String originalFileName = null;
        String savedFilePath = null;
        MultipartFile file = request.getFile();

        if (file != null && !file.isEmpty()) {
            originalFileName = file.getOriginalFilename();
            
            String uuid = UUID.randomUUID().toString();
            String extension = "";
            
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String savedFileName = uuid + extension;

            try {
                Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
                
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path targetLocation = uploadPath.resolve(savedFileName);

                file.transferTo(targetLocation.toFile());
                
                savedFilePath = "/uploads/inquiries/" + savedFileName; 
                
            } catch (IOException e) {
                log.error("파일 저장 실패: {}", e.getMessage(), e);
                throw new InoutException("파일 업로드 중 오류가 발생했습니다.", 500, "FILE_UPLOAD_ERROR");
            }
        }

        Inquiry inquiry = Inquiry.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .author(user)
                .originalFileName(originalFileName) 
                .savedFilePath(savedFilePath)       
                .build();

        return inquiryRepository.save(inquiry).getId();
    }

    public Page<InquiryListResponse> getInquiryList(Long userId, boolean isAdmin, Pageable pageable) {
        Page<Inquiry> inquiries;

        if (isAdmin) {
            inquiries = inquiryRepository.findAllWithAuthor(pageable);
        } else {
            inquiries = inquiryRepository.findAllWithAuthorByUserId(userId, pageable);
        }

        return inquiries.map(InquiryListResponse::from);
    }


    @Transactional
    public InquiryDetailResponse getInquiryDetail(Long inquiryId, Long userId, boolean isAdmin) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new InoutException("문의글을 찾을 수 없습니다.", 404, "INQUIRY_NOT_FOUND"));

        if (!isAdmin && !inquiry.getAuthor().getId().equals(userId)) {
            throw new InoutException("조회 권한이 없습니다.", 403, "FORBIDDEN");
        }

        if (isAdmin && !inquiry.isRead()) {
            inquiry.markAsRead();
        }

        return InquiryDetailResponse.from(inquiry);
    }

    @Transactional
    public void deleteInquiry(Long inquiryId, Long userId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new InoutException("문의글을 찾을 수 없습니다.", 404, "INQUIRY_NOT_FOUND"));

        if (!inquiry.getAuthor().getId().equals(userId)) {
            throw new InoutException("삭제 권한이 없습니다.", 403, "FORBIDDEN");
        }

        inquiryRepository.delete(inquiry);
    }
}