package com.jstudy.inout.common.auth.service;

import org.springframework.data.domain.Pageable;
import com.jstudy.inout.common.auth.dto.AdminUserDto;
import com.jstudy.inout.common.auth.dto.UserInput;
import com.jstudy.inout.common.auth.dto.UserPasswordResetInput;
import com.jstudy.inout.common.auth.dto.UserProfileResponse;
import com.jstudy.inout.common.auth.dto.UserProfileUpdateRequest;
import com.jstudy.inout.common.auth.dto.UserUpdate;
import com.jstudy.inout.common.dto.ServiceResult;
import jakarta.validation.Valid;

public interface AuthService {
    ServiceResult addUser(UserInput userInput); 
    
    ServiceResult updateUser(Long id, UserUpdate userUpdate);
    
    void checkEmail(String email);
    
    ServiceResult resetPassword(UserPasswordResetInput userInput);    
    
    ServiceResult completePasswordReset(String uuid, String newPassword);
    
    void unlockUser(Long id);
    
    void loginFailed(String email);
    
    void loginSuccess(String email);
    
    UserProfileResponse getMyProfile(Long id);
    
    void updateMyProfile(Long id, @Valid UserProfileUpdateRequest request);
   
    void updateUserByAdmin(Long id, AdminUserDto.UpdateRequest request);
    
    AdminUserDto.ListResponse getAdminUserList(Long storeId, String status, String keyword, Pageable pageable);
    
    void sendPasswordResetMailByAdmin(Long userId);
}