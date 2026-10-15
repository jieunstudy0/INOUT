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
import com.jstudy.inout.common.exception.ErrorCode;
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
	public ResponseEntity<?> resetPassword(@RequestBody @Valid UserPasswordResetInput userPasswordResetInput) {
	    log.info("📩 비밀번호 초기화 요청 이메일: {}", userPasswordResetInput.getEmail());

	    // 실패(회원 없음·템플릿 없음·메일 오류)는 예외로 던져 GlobalExceptionHandler가 상태 코드·resultCode를 그대로 응답한다.
	    authService.resetPassword(userPasswordResetInput);

	    log.info("✅ 비밀번호 초기화 이메일 전송 완료: {}", userPasswordResetInput.getEmail());
	    return ResponseResult.successWithMessage("비밀번호 초기화 이메일이 전송되었습니다.");
	}
	

	@PostMapping("/resetPassword")
	public ResponseEntity<?> resetPassword(
	        @RequestParam("resetKey") String resetKey,
	        @RequestParam("newPassword") String newPassword,
	        @RequestParam("confirmPassword") String confirmPassword) {
	   
	    if (!newPassword.equals(confirmPassword)) {
	        return ResponseResult.fail(ErrorCode.PASSWORD_MISMATCH);
	    }
	    
	    ServiceResult result = authService.completePasswordReset(resetKey, newPassword);

	    Map<String, String> data = Map.of("redirectUrl", "/user/login");
	    return ResponseResult.success(result.getMessage(), data);
	}
 
 
	@GetMapping("/public/password/reset/check")
	public ResponseEntity<?> checkResetKey(@RequestParam("key") String passwordResetKey) {
	 
	    User user = userRepository.findByPasswordResetKey(passwordResetKey)
	            .orElseThrow(() -> new InoutException(ErrorCode.INVALID_LINK));

	    if (user.isPasswordResetExpired(LocalDateTime.now())) {
	        return ResponseResult.fail(ErrorCode.EXPIRED_LINK);
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
	        return ResponseResult.fail(ErrorCode.NOT_OWNER, "본인의 정보만 수정할 수 있습니다.");
	    }

	    if (errors.hasErrors()) {
	        return ResponseResult.failWithData(ErrorCode.VALIDATION_ERROR, ResponseError.of(errors.getAllErrors()));
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
	        return ResponseResult.failWithData(ErrorCode.VALIDATION_ERROR, ResponseError.of(errors.getAllErrors()));
	    }
	    
	    if (!principal.getUser().getId().equals(id)) {
	        return ResponseResult.fail(ErrorCode.NOT_OWNER, "본인의 정보만 수정할 수 있습니다.");
	    }

	    User user = userRepository.findById(id)
	            .orElseThrow(() -> new UserNotFoundException("사용자 정보를 찾을 수 없습니다."));

	    if (!passwordEncoder.matches(userInputPassword.getPassword(), user.getPassword())) {
	        return ResponseResult.fail(ErrorCode.CURRENT_PASSWORD_MISMATCH);
	    }

	    user.changePassword(passwordEncoder.encode(userInputPassword.getNewPassword()));
	    userRepository.save(user);

	    return ResponseResult.successWithMessage("비밀번호가 성공적으로 변경되었습니다.");
	}
	
    @GetMapping("/profile")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            return ResponseResult.fail(ErrorCode.UNAUTHORIZED);
        }
        
        UserProfileResponse profile = authService.getMyProfile(principal.getUser().getId());
        return ResponseResult.success("내 정보 조회 성공", profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UserProfileUpdateRequest request,
            Errors errors) {

        if (principal == null) return ResponseResult.fail(ErrorCode.UNAUTHORIZED);
        if (errors.hasErrors()) return ResponseResult.failWithData(ErrorCode.VALIDATION_ERROR, ResponseError.of(errors.getAllErrors()));

        authService.updateMyProfile(principal.getUser().getId(), request);
        return ResponseResult.successWithMessage("내 정보가 성공적으로 수정되었습니다.");
    }

    @PatchMapping("/profile/password")
    public ResponseEntity<?> updateMyPassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid UserInputPassword request, 
            Errors errors) {

        if (principal == null) return ResponseResult.fail(ErrorCode.UNAUTHORIZED);
        if (errors.hasErrors()) return ResponseResult.failWithData(ErrorCode.VALIDATION_ERROR, ResponseError.of(errors.getAllErrors()));

       
        User user = userRepository.findById(principal.getUser().getId())
                .orElseThrow(() -> new UserNotFoundException("사용자 정보를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseResult.fail(ErrorCode.CURRENT_PASSWORD_MISMATCH);
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseResult.successWithMessage("비밀번호가 성공적으로 변경되었습니다.");
    }
}
