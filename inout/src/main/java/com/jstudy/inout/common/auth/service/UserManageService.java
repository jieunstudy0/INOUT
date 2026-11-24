package com.jstudy.inout.common.auth.service;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.jstudy.inout.common.auth.dto.UserDetailDto.BasicInfoRequest;
import com.jstudy.inout.common.auth.dto.UserDetailDto.Detail;
import com.jstudy.inout.common.auth.dto.UserDetailDto.StoreOption;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.StoreRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
import lombok.RequiredArgsConstructor;

/**
 * 직원 상세 화면용 조회와 기본 정보(이름·연락처·사진) 수정.
 * 상태·권한·예치금 한도 같은 기존 관리 기능은 AuthService에 그대로 있고, 화면에서 함께 부른다.
 * 점주는 소속 매장 직원만 보고 고칠 수 있다 (CROSS_STORE_FORBIDDEN).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserManageService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    public Detail getDetailForAdmin(Long userId) {
        return Detail.from(loadUser(userId));
    }

    @Transactional
    public void updateBasicInfoByAdmin(Long userId, BasicInfoRequest request) {
        loadUser(userId).changeBasicInfo(request.getName().trim(), request.getPhone().trim(), blankToNull(request.getProfileImageUrl()));
    }

    public Detail getDetailForOwner(Long ownerId, Long userId) {
        return Detail.from(requireSameStore(ownerId, userId));
    }

    @Transactional
    public void updateBasicInfoByOwner(Long ownerId, Long userId, BasicInfoRequest request) {
        requireSameStore(ownerId, userId)
                .changeBasicInfo(request.getName().trim(), request.getPhone().trim(), blankToNull(request.getProfileImageUrl()));
    }

    /** 본사 직원 상세 화면의 소속 매장 선택 목록 */
    public List<StoreOption> getStores() {
        return storeRepository.findAll().stream()
                .sorted(Comparator.comparing(s -> s.getId()))
                .map(s -> new StoreOption(s.getId(), s.getName()))
                .toList();
    }

    private User requireSameStore(Long ownerId, Long userId) {
        User owner = loadUser(ownerId);
        if (owner.getStore() == null) {
            throw new InoutException(ErrorCode.STORE_REQUIRED);
        }
        User target = loadUser(userId);
        if (target.getStore() == null || !owner.getStore().getId().equals(target.getStore().getId())) {
            throw new InoutException(ErrorCode.CROSS_STORE_FORBIDDEN, "소속 매장의 직원만 관리할 수 있습니다.");
        }
        return target;
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new InoutException(ErrorCode.USER_NOT_FOUND));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
