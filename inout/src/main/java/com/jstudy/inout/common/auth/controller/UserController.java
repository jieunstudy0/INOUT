package com.jstudy.inout.common.auth.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.auth.dto.UserInput;
import com.jstudy.inout.common.auth.dto.UserInputFind;
import com.jstudy.inout.common.auth.dto.UserInputPassword;
import com.jstudy.inout.common.auth.dto.UserPasswordResetInput;
import com.jstudy.inout.common.auth.dto.UserProfileResponse;
import com.jstudy.inout.common.auth.dto.UserProfileUpdateRequest;
import com.jstudy.inout.common.auth.dto.UserResponse;
import com.jstudy.inout.common.auth.dto.UserUpdate;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.auth.service.AuthService;
import com.jstudy.inout.common.dto.ResponseError;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.common.dto.ServiceResult;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.common.exception.UserNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Tag(name = "인증 (Auth)", description = "회원가입, 이메일 중복확인, 비밀번호 재설정, 정보 수정")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {

	private final AuthService authService;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@PostMapping("/register")
	public ResponseEntity<?> addUser(@Valid @RequestBody UserInput userInput) {
	    
	    authService.addUser(userInput);
	    Map<String, String> data = new HashMap<>();
	    data.put("redirectUrl", "/user/login");

	    return ResponseResult.success("회원가입 성공!", data);
	}

	@GetMapping("/public/check-email")
	public ResponseEntity<?> checkEmailDuplicate(@RequestParam("email") String email) {
	    authService.checkEmail(email); 
	    return ResponseResult.successWithMessage("사용 가능한 이메일입니다."); 
	}
	

	@PostMapping("/find")
	public ResponseEntity<?> findUser(@RequestBody UserInputFind userInputFind) {
	    
		User user = userRepository.findByNameAndPhone(
	            userInputFind.getName(), userInputFind.getPhone())
	            .orElseThrow(() -> new UserNotFoundException("입력하신 정보와 일치하는 이메일을 찾을 수 없습니다."));

	    UserResponse userResponse = UserResponse.of(user);

	    return ResponseResult.success("사용자 정보를 성공적으로 조회했습니다.", userResponse);
	}

	@PostMapping("/public/password/reset")
	public ResponseEntity<?> resetPassword(@RequestBody @Valid UserPasswordResetInput userPasswordResetInput, Errors errors) {
	    log.info("📩 비밀번호 초기화 요청 이메일: {}", userPasswordResetInput.getEmail());

	    if (errors.hasErrors()) {
	        return ResponseResult.fail("입력값이 정확하지 않습니다.", ResponseError.of(errors.getAllErrors()));
	    }

	    try {
	        ServiceResult result = authService.resetPassword(userPasswordResetInput);

	        if (result.isFail()) {
	            log.warn("❌ 비밀번호 초기화 실패: {}", result.getMessage());
	            return ResponseResult.fail(result.getMessage()); // 서비스에서 "사용자 정보를 찾을 수 없습니다" 등을 리턴하도록 설계
	        } 
	        
	        log.info("✅ 비밀번호 초기화 이메일 전송 완료: {}", userPasswordResetInput.getEmail());
	        return ResponseResult.successWithMessage("비밀번호 초기화 이메일이 전송되었습니다.");

	    } catch (InoutException e) {
	        log.error("🚨 비밀번호 초기화 중 커스텀 예외 발생: {}", e.getMessage());
	        return ResponseResult.fail(e.getMessage());
	    } catch (Exception e) {
	
	        log.error("🚨 시스템 에러 발생: ", e);
	        return ResponseResult.fail("서버 내부에서 예상치 못한 오류가 발생했습니다.");
	    }
	}
	

	@PostMapping("/resetPassword")
	public ResponseEntity<?> resetPassword(
	        @RequestParam("resetKey") String resetKey,
	        @RequestParam("newPassword") String newPassword,
	        @RequestParam("confirmPassword") String confirmPassword) {
	   
	    if (!newPassword.equals(confirmPassword)) {
	        return ResponseResult.fail("비밀번호가 일치하지 않습니다.");
	    }
	    
	    ServiceResult result = authService.completePasswordReset(resetKey, newPassword);

	    if (result.isFail()) {
	        return ResponseResult.fail(result.getMessage());
	    }

	    Map<String, String> data = Map.of("redirectUrl", "/user/login");
	    return ResponseResult.success(result.getMessage(), data);
	}
 
 
	@GetMapping("/public/password/reset/check")
	public ResponseEntity<?> checkResetKey(@RequestParam("key") String passwordResetKey) {
	 
	    User user = userRepository.findByPasswordResetKey(passwordResetKey)
	            .orElseThrow(() -> new InoutException("유효하지 않은 재설정 링크입니다.", 400, "INVALID_LINK"));

	    LocalDateTime now = LocalDateTime.now();
	    if (user.getUpdatedAt().plusMinutes(30).isBefore(now)) {

	    	user.clearPasswordResetInfo();
	        userRepository.save(user);
	        return ResponseResult.fail("링크 유효 시간이 만료되었습니다.", "EXPIRED_LINK", 400);
	    }
	    return ResponseResult.success("유효한 접근입니다.", Map.of("resetKey", passwordResetKey));
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<?> updateUser(
			@PathVariable("id") Long id,
			@AuthenticationPrincipal CustomUserDetails principal,
			@RequestBody @Valid UserUpdate userUpdate,
			Errors errors) {

	    if (principal == null || !principal.getUser().getId().equals(id)) {
	        return ResponseResult.fail("본인 정보만 수정할 수 있습니다.", 403);
	    }

	    if (errors.hasErrors()) {
	        return ResponseResult.fail("입력값이 정확하지 않습니다.", ResponseError.of(errors.getAllErrors()));
	    }
	    authService.updateUser(id, userUpdate);

	    return ResponseResult.successWithMessage("사용자 정보가 성공적으로 수정되었습니다.");
	}

	@PatchMapping("/{id}/password")
	public ResponseEntity<?> updateUserPassword(@PathVariable("id") Long id, 
			@AuthenticationPrincipal CustomUserDetails principal,
	                                            @RequestBody @Valid UserInputPassword userInputPassword, 
	                                            Errors errors) {

	    if (errors.hasErrors()) {
	        return ResponseResult.fail("입력값이 규격에 맞지 않습니다.", ResponseError.of(errors.getAllErrors()));
	    }
	    
	    if (!principal.getUser().getId().equals(id)) {
	        return ResponseResult.fail("본인의 정보만 수정할 수 있습니다.", 403);
	    }

	    User user = userRepository.findById(id)
	            .orElseThrow(() -> new UserNotFoundException("사용자 정보가 없습니다."));

	    if (!passwordEncoder.matches(userInputPassword.getPassword(), user.getPassword())) {
	        return ResponseResult.fail("현재 비밀번호가 일치하지 않습니다.");
	    }

	    user.changePassword(passwordEncoder.encode(userInputPassword.getNewPassword()));
	    userRepository.save(user);

	    return ResponseResult.successWithMessage("비밀번호가 성공적으로 변경되었습니다.");
	}
	
    @GetMapping("/profile")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            return ResponseResult.fail("인증 정보가 없습니다.", 401);
        }
        
        UserProfileResponse profile = authService.getMyProfile(principal.getUser().getId());
        return ResponseResult.success("내 정보 조회 성공", profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UserProfileUpdateRequest request,
            Errors errors) {

        if (principal == null) return ResponseResult.fail("인증 정보가 없습니다.", 401);
        if (errors.hasErrors()) return ResponseResult.fail("입력값이 올바르지 않습니다.", ResponseError.of(errors.getAllErrors()));

        authService.updateMyProfile(principal.getUser().getId(), request);
        return ResponseResult.successWithMessage("내 정보가 성공적으로 수정되었습니다.");
    }

    @PatchMapping("/profile/password")
    public ResponseEntity<?> updateMyPassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid UserInputPassword request, 
            Errors errors) {

        if (principal == null) return ResponseResult.fail("인증 정보가 없습니다.", 401);
        if (errors.hasErrors()) return ResponseResult.fail("입력값이 올바르지 않습니다.", ResponseError.of(errors.getAllErrors()));

       
        User user = userRepository.findById(principal.getUser().getId())
                .orElseThrow(() -> new UserNotFoundException("사용자 정보가 없습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseResult.fail("현재 비밀번호가 일치하지 않습니다.");
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseResult.successWithMessage("비밀번호가 성공적으로 변경되었습니다.");
    }
}
