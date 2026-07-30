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
import com.jstudy.inout.common.auth.dto.OwnerUserDto;
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

    @Value("${app.server-url:http://localhost:8080}")
    private String serverUrl;

    @Value("${app.mail.from-email:noreply@inout.com}")
    private String fromEmail;

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

    @Override
    @Transactional(readOnly = true)
    public OwnerUserDto.ListResponse getOwnerUserList(Long ownerUserId, String status, String keyword, Pageable pageable) {
        User owner = requireOwnerWithStore(ownerUserId);

        UserStatus userStatus = parseUserStatus(status);
        Page<User> userPage = userRepository.findOwnerUsersByFilters(owner.getStore().getId(), userStatus, keyword, pageable);

        long total = userRepository.countByStore_Id(owner.getStore().getId());
        long active = userPage.getContent().stream().filter(u -> u.getStatus() == UserStatus.ACTIVE).count();
        long leave = userPage.getContent().stream().filter(u -> u.getStatus() == UserStatus.LEAVE).count();
        long resigned = userPage.getContent().stream().filter(u -> u.getStatus() == UserStatus.RESIGNED).count();
        long locked = userRepository.countByStore_IdAndIsLockedTrue(owner.getStore().getId());

        OwnerUserDto.Summary summary = OwnerUserDto.Summary.builder()
                .total(total)
                .active(active)
                .leave(leave)
                .resigned(resigned)
                .locked(locked)
                .build();

        Page<OwnerUserDto.UserListItem> users = userPage.map(u -> OwnerUserDto.UserListItem.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .storeId(u.getStore() != null ? u.getStore().getId() : null)
                .storeName(u.getStore() != null ? u.getStore().getName() : null)
                .status(u.getStatus())
                .isLocked(u.isLocked())
                .roleName(resolvePrimaryRoleName(u))
                .createdAt(u.getCreatedAt())
                .build());

        return OwnerUserDto.ListResponse.builder().summary(summary).users(users).build();
    }

    @Override
    @Transactional
    public ServiceResult createEmployeeByOwner(Long ownerUserId, OwnerUserDto.CreateRequest request) {
        User owner = requireOwnerWithStore(ownerUserId);

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new InoutException("비밀번호 확인이 일치하지 않습니다.", 400, "PASSWORD_CONFIRM_MISMATCH");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new InoutException("이미 사용 중인 이메일입니다.", 400, "DUPLICATE_EMAIL");
        }

        User employee = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .birthday(request.getBirthday())
                .store(owner.getStore())
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(employee);

        Role employeeRole = roleRepository.findByRoleName("ROLE_EMPLOYEE")
                .orElseThrow(() -> new InoutException("기본 권한 정보를 찾을 수 없습니다.", 500, "ROLE_NOT_FOUND"));
        userRoleRepository.save(UserRole.builder().user(employee).role(employeeRole).build());

        return ServiceResult.success("직원 계정이 생성되었습니다.");
    }

    @Override
    @Transactional
    public void updateEmployeeByOwner(Long ownerUserId, Long employeeUserId, OwnerUserDto.UpdateRequest request) {
        User owner = requireOwnerWithStore(ownerUserId);
        User employee = requireSameStoreEmployee(owner, employeeUserId);
        employee.updateStatusAndStore(request.getStatus(), owner.getStore());
    }

    @Override
    @Transactional
    public void unlockEmployeeByOwner(Long ownerUserId, Long employeeUserId) {
        User owner = requireOwnerWithStore(ownerUserId);
        User employee = requireSameStoreEmployee(owner, employeeUserId);
        employee.resetLoginAttributes();
    }

    @Override
    @Transactional
    public void sendPasswordResetMailByOwner(Long ownerUserId, Long employeeUserId) {
        User owner = requireOwnerWithStore(ownerUserId);
        User employee = requireSameStoreEmployee(owner, employeeUserId);

        UserPasswordResetInput input = new UserPasswordResetInput();
        input.setEmail(employee.getEmail());
        input.setName(employee.getName());
        input.setPhone(employee.getPhone());
        this.resetPassword(input);
    }

    private User requireOwnerWithStore(Long ownerUserId) {
        User owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404, "USER_NOT_FOUND"));
        if (owner.getStore() == null) {
            throw new InoutException("소속 매장 정보가 없습니다.", 403, "STORE_REQUIRED");
        }
        return owner;
    }

    private User requireSameStoreEmployee(User owner, Long employeeUserId) {
        User employee = userRepository.findById(employeeUserId)
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404, "USER_NOT_FOUND"));
        if (employee.getStore() == null || !owner.getStore().getId().equals(employee.getStore().getId())) {
            throw new InoutException("다른 매장 직원에는 접근할 수 없습니다.", 403, "CROSS_STORE_FORBIDDEN");
        }
        return employee;
    }

    private UserStatus parseUserStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return UserStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String resolvePrimaryRoleName(User user) {
        return user.getUserRoles().stream()
                .map(ur -> ur.getRole().getRoleName())
                .findFirst()
                .orElse("ROLE_EMPLOYEE");
    }
}