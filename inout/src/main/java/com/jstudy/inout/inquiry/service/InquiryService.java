package com.jstudy.inout.inquiry.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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
import com.jstudy.inout.inquiry.entity.InquiryTargetType;
import com.jstudy.inout.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.MalformedURLException;
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

        InquiryTargetType targetType = request.getTargetType() != null
                ? request.getTargetType()
                : InquiryTargetType.ADMIN;

        Inquiry inquiry = Inquiry.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .author(user)
                .targetType(targetType)
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

    // EMP: 본사/점주 대상별 조회
    public Page<InquiryListResponse> getEmpInquiriesByTarget(Long userId, InquiryTargetType targetType, Pageable pageable) {
        return inquiryRepository.findByAuthorIdAndTargetType(userId, targetType, pageable)
                .map(InquiryListResponse::from);
    }

    // OWNER: 매장 직원이 점주에게 보낸 문의 (targetType = OWNER)
    public Page<InquiryListResponse> getOwnerInquiriesFromStaff(Long storeId, Pageable pageable) {
        return inquiryRepository.findByTargetTypeAndStoreId(InquiryTargetType.OWNER, storeId, pageable)
                .map(InquiryListResponse::from);
    }

    // OWNER: 매장(점주+직원)이 본사로 보낸 문의 (targetType = ADMIN)
    public Page<InquiryListResponse> getOwnerInquiriesToAdmin(Long storeId, Pageable pageable) {
        return inquiryRepository.findByTargetTypeAndStoreId(InquiryTargetType.ADMIN, storeId, pageable)
                .map(InquiryListResponse::from);
    }

    // ADMIN: 점주가 본사로 보낸 문의
    public Page<InquiryListResponse> getAdminInquiriesFromOwners(Pageable pageable) {
        return inquiryRepository.findAdminInquiriesByWriterRole("ROLE_OWNER", pageable)
                .map(InquiryListResponse::from);
    }

    // ADMIN: 직원이 본사로 보낸 문의
    public Page<InquiryListResponse> getAdminInquiriesFromEmployees(Pageable pageable) {
        return inquiryRepository.findAdminInquiriesByWriterRole("ROLE_EMPLOYEE", pageable)
                .map(InquiryListResponse::from);
    }

    @Transactional
    public InquiryDetailResponse getInquiryDetail(Long inquiryId, Long userId, boolean isAdmin) {
        return getInquiryDetail(inquiryId, userId, isAdmin, null);
    }

    @Transactional
    public InquiryDetailResponse getInquiryDetail(Long inquiryId, Long userId, boolean isAdmin, Long ownerStoreId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new InoutException("문의글을 찾을 수 없습니다.", 404, "INQUIRY_NOT_FOUND"));

        boolean isAuthor = inquiry.getAuthor().getId().equals(userId);
        boolean isOwnerOfStore = ownerStoreId != null
                && inquiry.getAuthor().getStore() != null
                && ownerStoreId.equals(inquiry.getAuthor().getStore().getId());

        if (!isAdmin && !isAuthor && !isOwnerOfStore) {
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
            throw new InoutException("삭제 권한이 없습니다. 작성자 본인만 삭제할 수 있습니다.", 403, "FORBIDDEN");
        }

        inquiryRepository.delete(inquiry);
    }

    /**
     * 문의 첨부파일 다운로드용 리소스 조회.
     * 작성자 본인 또는 관리자만 접근할 수 있습니다.
     */
    public InquiryFileResource getInquiryFileResource(Long inquiryId, Long userId, boolean isAdmin) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new InoutException("문의글을 찾을 수 없습니다.", 404, "INQUIRY_NOT_FOUND"));

        if (!isAdmin && !inquiry.getAuthor().getId().equals(userId)) {
            throw new InoutException("다운로드 권한이 없습니다.", 403, "FORBIDDEN");
        }

        String savedFilePath = inquiry.getSavedFilePath();
        String originalFileName = inquiry.getOriginalFileName();
        if (savedFilePath == null || savedFilePath.isBlank()
                || originalFileName == null || originalFileName.isBlank()) {
            throw new InoutException("첨부파일이 존재하지 않습니다.", 404, "FILE_NOT_FOUND");
        }

        try {
            Path filePath = resolveStoredFilePath(savedFilePath);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("첨부파일 리소스를 읽을 수 없음. inquiryId={}, path={}", inquiryId, filePath);
                throw new InoutException("첨부파일을 찾을 수 없습니다.", 404, "FILE_NOT_FOUND");
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }

            return new InquiryFileResource(resource, originalFileName, contentType);
        } catch (MalformedURLException e) {
            log.error("첨부파일 URL 생성 실패: {}", e.getMessage(), e);
            throw new InoutException("첨부파일 다운로드 중 오류가 발생했습니다.", 500, "FILE_DOWNLOAD_ERROR");
        } catch (IOException e) {
            log.error("첨부파일 Content-Type 조회 실패: {}", e.getMessage(), e);
            throw new InoutException("첨부파일 다운로드 중 오류가 발생했습니다.", 500, "FILE_DOWNLOAD_ERROR");
        }
    }

    private Path resolveStoredFilePath(String savedFilePath) {
        Path configuredUploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        String fileName = Paths.get(savedFilePath).getFileName().toString();
        Path candidate = configuredUploadDir.resolve(fileName).normalize();

        if (!candidate.startsWith(configuredUploadDir)) {
            throw new InoutException("잘못된 파일 경로입니다.", 400, "INVALID_FILE_PATH");
        }
        return candidate;
    }

    public record InquiryFileResource(Resource resource, String originalFileName, String contentType) {}
}