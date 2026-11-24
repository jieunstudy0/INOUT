package com.jstudy.inout.request.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.request.dto.ChangeRequestDtos.CreateRequest;
import com.jstudy.inout.request.dto.ChangeRequestDtos.StatusRequest;
import com.jstudy.inout.request.entity.ChangeRequestStatus;
import com.jstudy.inout.request.service.ActorRole;
import com.jstudy.inout.request.service.ChangeRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 현장 수정 요청. 모든 역할이 같은 주소를 쓰고, 누가 무엇을 볼·바꿀 수 있는지는 서비스가 역할과 매장으로 판단한다.
 */
@Tag(name = "수정 요청", description = "현장 직원·점주가 남기는 수정 요청과 점주·본사의 처리")
@RestController
@RequestMapping("/api/change-requests")
@RequiredArgsConstructor
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;

    @Operation(summary = "수정 요청 남기기",
            description = "multipart: request(JSON) + photo(선택, jpg·png·webp, 10MB 이하). 받는 쪽(점주/본사)은 종류로 정해집니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(@AuthenticationPrincipal CustomUserDetails principal,
                                    @RequestPart("request") @Valid CreateRequest request,
                                    @RequestPart(value = "photo", required = false) MultipartFile photo) {
        Long id = changeRequestService.create(principal.getUser().getId(), role(principal), request, photo);
        return ResponseResult.success("수정 요청이 접수되었습니다.", id);
    }

    @Operation(summary = "내가 보낸 요청")
    @GetMapping("/mine")
    public ResponseEntity<?> getMine(@AuthenticationPrincipal CustomUserDetails principal,
                                     @RequestParam(name = "page", defaultValue = "0") int page) {
        return ResponseResult.success("보낸 요청 조회가 완료되었습니다.",
                changeRequestService.getMine(principal.getUser().getId(), page));
    }

    @Operation(summary = "받은 요청함 (점주·본사)")
    @GetMapping("/inbox")
    public ResponseEntity<?> getInbox(@AuthenticationPrincipal CustomUserDetails principal,
                                      @RequestParam(name = "status", required = false) ChangeRequestStatus status,
                                      @RequestParam(name = "page", defaultValue = "0") int page) {
        return ResponseResult.success("받은 요청 조회가 완료되었습니다.",
                changeRequestService.getInbox(principal.getUser().getId(), role(principal), status, page));
    }

    @Operation(summary = "요청 상세")
    @GetMapping("/{requestId}")
    public ResponseEntity<?> getDetail(@AuthenticationPrincipal CustomUserDetails principal,
                                       @PathVariable("requestId") Long requestId) {
        return ResponseResult.success("요청 조회가 완료되었습니다.",
                changeRequestService.getDetail(principal.getUser().getId(), role(principal), requestId));
    }

    @Operation(summary = "요청 상태 변경 (처리 중·완료·반려)", description = "반려는 사유가 필요합니다. 완료·반려된 요청은 다시 바꿀 수 없습니다.")
    @PatchMapping("/{requestId}/status")
    public ResponseEntity<?> changeStatus(@AuthenticationPrincipal CustomUserDetails principal,
                                          @PathVariable("requestId") Long requestId,
                                          @RequestBody @Valid StatusRequest request) {
        changeRequestService.changeStatus(principal.getUser().getId(), role(principal), requestId, request);
        return ResponseResult.successWithMessage("요청 상태가 변경되었습니다.");
    }

    private static ActorRole role(CustomUserDetails principal) {
        return ActorRole.from(principal.getAuthorities());
    }
}
