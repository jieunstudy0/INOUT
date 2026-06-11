package com.jstudy.inout.common.auth.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.jstudy.inout.common.auth.dto.AdminUserDto;
import com.jstudy.inout.common.auth.dto.UserInput;
import com.jstudy.inout.common.auth.dto.UserPasswordResetInput;
import com.jstudy.inout.common.auth.dto.UserUpdate;
import com.jstudy.inout.common.auth.entity.Role;
import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.entity.UserRole;
import com.jstudy.inout.common.auth.entity.UserStatus;
import com.jstudy.inout.common.auth.repository.RoleRepository;
import com.jstudy.inout.common.auth.repository.StoreRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.auth.repository.UserRoleRepository;
import com.jstudy.inout.common.dto.ServiceResult;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.common.exception.UserNotFoundException;
import com.jstudy.inout.common.mail.config.MailComponent;
import com.jstudy.inout.common.mail.dto.MailTemplate;
import com.jstudy.inout.common.mail.repository.MailTemplateRepository;
import com.jstudy.inout.common.auth.dto.UserProfileResponse;
import com.jstudy.inout.common.auth.dto.UserProfileUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@RequiredArgsConstructor
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final MailComponent mailComponent;
    private final MailTemplateRepository mailTemplateRepository;
    private final PasswordEncoder passwordEncoder;
    private final StoreRepository storeRepository;

    @Value("${app.server-url}")
    private String serverUrl;

    @Transactional
    @Override
    public ServiceResult addUser(UserInput userInput) {
        Optional<User> optionalUser = userRepository.findByEmail(userInput.getEmail());
        if (optionalUser.isPresent()) {
            throw new InoutException("이미 사용 중인 이메일입니다.", 400, "DUPLICATE_EMAIL");
        }

        Store store = storeRepository.findById(userInput.getStoreId())
                .orElseThrow(() -> new InoutException("존재하지 않는 매장입니다.", 404, "STORE_NOT_FOUND"));

        String encryptPassword = passwordEncoder.encode(userInput.getPassword());

        User user = User.builder()
                .email(userInput.getEmail())
                .name(userInput.getName())
                .password(encryptPassword)
                .phone(userInput.getPhone())
                .store(store)
                .birthday(userInput.getBirthday())   
                .build();
        userRepository.save(user);

        Role defaultRole = roleRepository.findByRoleName("ROLE_EMPLOYEE")
            .orElseThrow(() -> new InoutException("기본 권한 정보를 찾을 수 없습니다."));
       
        UserRole userRole = UserRole.builder()
            .user(user)
            .role(defaultRole)
            .build();
        userRoleRepository.save(userRole);

        String fromEmail = "jieunstudy@kakao.com";
        String fromName = "관리자";
        String toEmail = user.getEmail();
        String toName = user.getName();
        String title = "회원가입을 축하드립니다.";
        String contents = "회원가입을 축하드립니다.";

        boolean emailSent =  mailComponent.send(fromEmail, fromName, toEmail, toName, title, contents);
        if (!emailSent) {
            log.warn("회원가입 축하 메일 발송 실패: {}", user.getEmail());
        }
        return ServiceResult.success();
    }

    @Override
    public void checkEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new InoutException("이미 사용 중인 이메일입니다.", 400);
        }
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자 정보를 찾을 수 없습니다."));
        return UserProfileResponse.from(user);
    }

    @Transactional
    public void updateMyProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자 정보를 찾을 수 없습니다."));

        Store store = null;
        if (StringUtils.hasText(request.getStoreName())) {
            store = storeRepository.findByName(request.getStoreName())
                    .orElseThrow(() -> new InoutException("입력하신 매장명을 찾을 수 없습니다.", 404, "STORE_NOT_FOUND"));
        }
        user.updateProfile(request.getName(), request.getPhone(), store);

        if (StringUtils.hasText(request.getPassword())) {
            user.changePassword(passwordEncoder.encode(request.getPassword()));
        }
    }

    @Transactional
    @Override
    public ServiceResult resetPassword(UserPasswordResetInput userInput) {
        Optional<User> optionalUser = userRepository.findByEmailAndNameAndPhone(
            userInput.getEmail(), userInput.getName(), userInput.getPhone()
        );

        if (!optionalUser.isPresent()) {
            return ServiceResult.fail("사용자 정보를 찾을 수 없습니다.");
        }

        User user = optionalUser.get();
        String passwordResetKey = UUID.randomUUID().toString();
        user.setPasswordResetInfo(passwordResetKey);
        userRepository.save(user);

        MailTemplate mailTemplate = mailTemplateRepository.findByTemplateId("USER_RESET_PASSWORD")
            .orElseThrow(() -> new InoutException("메일 템플릿이 존재하지 않습니다."));

        String fromEmail = mailTemplate.getSendEmail();
        String fromUserName = mailTemplate.getSendUserName();
        String title = mailTemplate.getTitle().replaceAll("\\{USER_NAME\\}", user.getName());
        String contents = mailTemplate.getContents()
            .replaceAll("\\{USER_NAME\\}", user.getName())
            .replaceAll("\\{SERVER_URL\\}", serverUrl)
            .replaceAll("\\{RESET_PASSWORD_KEY\\}", passwordResetKey);

        mailComponent.send(fromEmail, fromUserName, user.getEmail(), user.getName(), title, contents);
        return ServiceResult.success("비밀번호 초기화 이메일이 전송되었습니다. 이메일을 확인해 주세요.");
    }

    @Transactional
    public ServiceResult completePasswordReset(String uuid, String newPassword) {
        User user = userRepository.findByPasswordResetKey(uuid)
                .orElseThrow(() -> new InoutException("유효하지 않은 재설정 링크입니다."));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime requestTime = user.getUpdatedAt(); 

        if (requestTime.plusMinutes(30).isBefore(now)) {
            user.clearPasswordResetInfo();
            return ServiceResult.fail("링크 유효 시간(30분)이 만료되었습니다. 다시 시도해 주세요.");
        }

        user.changePassword(passwordEncoder.encode(newPassword));
        user.clearPasswordResetInfo();
        userRepository.save(user);

        return ServiceResult.success("비밀번호가 성공적으로 변경되었습니다.");
    }

    @Override
    @Transactional
    public ServiceResult updateUser(Long id, UserUpdate userUpdate) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new InoutException("사용자 정보가 없습니다.", 404, "USER_NOT_FOUND"));
        Store store = storeRepository.findById(userUpdate.getStoreId())
                .orElseThrow(() -> new InoutException("존재하지 않는 매장입니다.", 404, "STORE_NOT_FOUND"));
        user.updateInfo(userUpdate.getPhone(), store);
        return ServiceResult.success();
    }

    @Transactional
    public void loginFailed(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!user.isLocked()) user.increaseFailedAttempt();
        });
    }

    @Transactional
    public void loginSuccess(String email) {
        userRepository.findByEmail(email).ifPresent(User::resetLoginAttributes);
    }

    @Override
    @Transactional
    public void unlockUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404));
        user.resetLoginAttributes();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDto.ListResponse getAdminUserList(Long storeId, String status, String keyword, Pageable pageable) {
        long total = userRepository.count();
        long active = userRepository.countByStatus(UserStatus.ACTIVE);
        long leave = userRepository.countByStatus(UserStatus.LEAVE);
        long locked = userRepository.countByIsLockedTrue();

        AdminUserDto.Summary summary = AdminUserDto.Summary.builder()
                .total(total).active(active).leave(leave).locked(locked).build();

        UserStatus userStatus = null;
        if (StringUtils.hasText(status)) {
            try {
                userStatus = UserStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
            }
        }

        Page<User> userPage = userRepository.findAdminUsersByFilters(storeId, userStatus, keyword, pageable);

        Page<AdminUserDto.UserListItem> userItems = userPage.map(u -> {
            boolean isAdmin = u.getUserRoles().stream()
                    .anyMatch(ur -> ur.getRole().getRoleName().equals("ROLE_ADMIN"));

            return AdminUserDto.UserListItem.builder()
                    .id(u.getId())
                    .name(u.getName())
                    .email(u.getEmail())
                    .phone(u.getPhone())
                    .storeId(u.getStore() != null ? u.getStore().getId() : null)
                    .storeName(u.getStore() != null ? u.getStore().getName() : null)
                    .status(u.getStatus())
                    .isLocked(u.isLocked())
                    .isAdmin(isAdmin)
                    .createdAt(u.getCreatedAt())
                    .build();
        });

        return AdminUserDto.ListResponse.builder().summary(summary).users(userItems).build();
    }

    @Override
    @Transactional
    public void updateUserByAdmin(Long userId, AdminUserDto.UpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404));

        Store store = null;
        if (request.getStoreId() != null) {
            store = storeRepository.findById(request.getStoreId())
                    .orElseThrow(() -> new InoutException("존재하지 않는 매장입니다.", 404));
        }
        user.updateStatusAndStore(request.getStatus(), store);

        Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN")
                .orElseThrow(() -> new InoutException("관리자 권한 정보를 찾을 수 없습니다.", 500));

        boolean hasAdminRole = user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getRoleName().equals("ROLE_ADMIN"));

        if (request.isAdmin() && !hasAdminRole) {
            userRoleRepository.save(UserRole.builder().user(user).role(adminRole).build());
        } else if (!request.isAdmin() && hasAdminRole) {
            UserRole targetRole = user.getUserRoles().stream()
                    .filter(ur -> ur.getRole().getRoleName().equals("ROLE_ADMIN"))
                    .findFirst().orElse(null);
            if (targetRole != null) {
                userRoleRepository.delete(targetRole);
            }
        }
    }

    @Override
    @Transactional
    public void sendPasswordResetMailByAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404));
        
        UserPasswordResetInput input = new UserPasswordResetInput();
        input.setEmail(user.getEmail());
        input.setName(user.getName());
        input.setPhone(user.getPhone());
        this.resetPassword(input);
    }
}