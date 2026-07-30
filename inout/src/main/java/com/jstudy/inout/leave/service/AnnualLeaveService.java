package com.jstudy.inout.leave.service;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.leave.dto.AnnualLeaveDto;
import com.jstudy.inout.leave.entity.AnnualLeave;
import com.jstudy.inout.leave.entity.LeaveStatus;
import com.jstudy.inout.leave.entity.LeaveType;
import com.jstudy.inout.leave.repository.AnnualLeaveRepository;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnualLeaveService {

    /** 연간 기본 부여 일수 (승인 사용분 차감 기준) */
    public static final int DEFAULT_ANNUAL_LEAVE_DAYS = 15;

    private final AnnualLeaveRepository annualLeaveRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long submitLeave(Long userId, AnnualLeaveDto.CreateRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new InoutException("시작일은 종료일보다 늦을 수 없습니다.", 400, "INVALID_LEAVE_PERIOD");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404, "USER_NOT_FOUND"));

        boolean overlapping = annualLeaveRepository.existsOverlapping(
                userId, request.getStartDate(), request.getEndDate(), LeaveStatus.REJECTED);
        if (overlapping) {
            throw new InoutException("이미 신청된 연차 기간과 겹칩니다.", 400, "LEAVE_PERIOD_OVERLAP");
        }

        AnnualLeave leave = AnnualLeave.builder()
                .user(user)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .type(request.getType())
                .reason(request.getReason())
                .build();

        return annualLeaveRepository.save(leave).getId();
    }

    public Page<AnnualLeaveDto.ListItem> getMyLeaveList(Long userId, Pageable pageable) {
        return annualLeaveRepository.findByUserIdWithUser(userId, pageable).map(AnnualLeaveDto::toListItem);
    }

    public AnnualLeaveDto.DetailResponse getMyLeaveDetail(Long userId, Long leaveId) {
        AnnualLeave leave = annualLeaveRepository.findByIdWithUser(leaveId)
                .orElseThrow(() -> new InoutException("연차 신청 정보를 찾을 수 없습니다.", 404, "LEAVE_NOT_FOUND"));

        if (!leave.getUser().getId().equals(userId)) {
            throw new InoutException("본인의 연차 신청만 조회할 수 있습니다.", 403, "FORBIDDEN");
        }

        return AnnualLeaveDto.toDetail(leave);
    }

    public Page<AnnualLeaveDto.ListItem> getLeaveList(LeaveStatus status, Pageable pageable) {
        Page<AnnualLeave> page = (status != null)
                ? annualLeaveRepository.findByStatusWithUser(status, pageable)
                : annualLeaveRepository.findAllWithUser(pageable);
        return page.map(AnnualLeaveDto::toListItem);
    }

    public AnnualLeaveDto.DetailResponse getLeaveDetail(Long leaveId) {
        AnnualLeave leave = annualLeaveRepository.findByIdWithUser(leaveId)
                .orElseThrow(() -> new InoutException("연차 신청 정보를 찾을 수 없습니다.", 404, "LEAVE_NOT_FOUND"));
        return AnnualLeaveDto.toDetail(leave);
    }

    public Page<AnnualLeaveDto.ListItem> getLeaveListByStore(Long storeId, LeaveStatus status, Pageable pageable) {
        Page<AnnualLeave> page = (status != null)
                ? annualLeaveRepository.findByStoreIdAndStatusWithUser(storeId, status, pageable)
                : annualLeaveRepository.findByStoreIdWithUser(storeId, pageable);
        return page.map(AnnualLeaveDto::toListItem);
    }

    public AnnualLeaveDto.DetailResponse getLeaveDetailByStore(Long storeId, Long leaveId) {
        AnnualLeave leave = annualLeaveRepository.findByIdWithUser(leaveId)
                .orElseThrow(() -> new InoutException("연차 신청 정보를 찾을 수 없습니다.", 404, "LEAVE_NOT_FOUND"));
        assertSameStore(storeId, leave.getUser());
        return AnnualLeaveDto.toDetail(leave);
    }

    @Transactional
    public AnnualLeaveDto.DetailResponse processLeave(
            Long leaveId, Long processorId, AnnualLeaveDto.ProcessRequest request) {

        AnnualLeave leave = annualLeaveRepository.findByIdWithUser(leaveId)
                .orElseThrow(() -> new InoutException("연차 신청 정보를 찾을 수 없습니다.", 404, "LEAVE_NOT_FOUND"));

        User processor = userRepository.findById(processorId)
                .orElseThrow(() -> new InoutException("처리자 정보를 찾을 수 없습니다.", 404, "PROCESSOR_NOT_FOUND"));

        if (processor.getStore() == null) {
            throw new InoutException("소속 매장이 없는 계정은 연차를 처리할 수 없습니다.", 403, "STORE_REQUIRED");
        }
        assertSameStore(processor.getStore().getId(), leave.getUser());

        if (!leave.getStatus().isProcessable()) {
            throw new InoutException("이미 처리된 연차 신청입니다.", 400, "ALREADY_PROCESSED");
        }

        LeaveStatus newStatus = request.getStatus();

        if (newStatus == LeaveStatus.REJECTED) {
            if (!StringUtils.hasText(request.getRejectReason())) {
                throw new InoutException("반려 사유를 입력해야 합니다.", 400, "REJECT_REASON_REQUIRED");
            }
            leave.reject(processor, request.getRejectReason().trim());
        } else if (newStatus == LeaveStatus.APPROVED) {
            leave.approve(processor);
        } else if (newStatus == LeaveStatus.HOLD) {
            leave.hold(processor);
        } else {
            throw new InoutException("허용되지 않은 처리 상태입니다.", 400, "INVALID_LEAVE_STATUS");
        }

        return AnnualLeaveDto.toDetail(leave);
    }

    /**
     * 잔여 연차 = 기본 부여(15일) − 승인된 사용 일수.
     * 반차는 0.5일로 계산하며, 결과는 반올림한 정수로 반환한다.
     */
    public int getRemainingLeaveDays(Long userId) {
        List<AnnualLeave> approved = annualLeaveRepository.findByUserIdAndStatus(userId, LeaveStatus.APPROVED);
        double used = approved.stream().mapToDouble(this::toUsedDays).sum();
        return Math.max(0, (int) Math.round(DEFAULT_ANNUAL_LEAVE_DAYS - used));
    }

    private double toUsedDays(AnnualLeave leave) {
        if (leave.getType() == LeaveType.HALF_DAY) {
            return 0.5d;
        }
        long days = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
        return Math.max(days, 0);
    }

    private void assertSameStore(Long ownerStoreId, User targetUser) {
        if (targetUser.getStore() == null || !ownerStoreId.equals(targetUser.getStore().getId())) {
            throw new InoutException("다른 매장 직원의 연차에 접근할 수 없습니다.", 403, "CROSS_STORE_FORBIDDEN");
        }
    }
}
