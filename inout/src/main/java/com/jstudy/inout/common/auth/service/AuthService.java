package com.jstudy.inout.common.auth.service;

import com.jstudy.inout.common.auth.dto.UserInput;
import com.jstudy.inout.common.auth.dto.UserPasswordResetInput;
import com.jstudy.inout.common.auth.dto.UserUpdate;
import com.jstudy.inout.common.dto.ServiceResult;

public interface AuthService {
   
     ServiceResult addUser(UserInput userInput); 
     ServiceResult updateUser(Long id, UserUpdate userUpdate);
     void checkEmail(String email);
     ServiceResult resetPassword(UserPasswordResetInput userInput);	
     ServiceResult completePasswordReset(String uuid, String newPassword);

}
