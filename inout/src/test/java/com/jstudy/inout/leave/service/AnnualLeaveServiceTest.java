package com.jstudy.inout.leave.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.config.JpaAuditConfig;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.leave.dto.AnnualLeaveDto;
import com.jstudy.inout.leave.entity.AnnualLeave;
import com.jstudy.inout.leave.entity.LeaveStatus;
import com.jstudy.inout.leave.entity.LeaveType;
import com.jstudy.inout.leave.repository.AnnualLeaveRepository;
import com.jstudy.inout.order.testsupport.OrderJpaTestApplication;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ActiveProfiles("jpa-slice")
@ContextConfiguration(classes = OrderJpaTestApplication.class)
@Import({JpaAuditConfig.class, AnnualLeaveService.class})
class AnnualLeaveServiceTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnualLeaveRepository annualLeaveRepository;

    @Autowired
    private AnnualLeaveService annualLeaveService;

    private Store store;
    private User employee;
    private User admin;

    @BeforeEach
    void setUp() {
        store = Store.builder()
                .name("테스트매장")
                .address("서울시 강남구 테헤란로 1")
                .build();
        em.persist(store);

        employee = User.builder()
                .email("emp@test.com")
                .password("pw")
                .name("직원1")
                .phone("010-0000-0001")
                .birthday(LocalDate.of(1995, 1, 1))
                .store(store)
                .deleted(false)
                .build();
        em.persist(employee);

        admin = User.builder()
                .email("admin@test.com")
                .password("pw")
                .name("관리자1")
                .phone("010-0000-0002")
                .birthday(LocalDate.of(1985, 1, 1))
                .store(store)
                .deleted(false)
                .build();
        em.persist(admin);

        em.flush();
        em.clear();
    }

    private AnnualLeaveDto.CreateRequest createRequest(LocalDate start, LocalDate end) {
        return AnnualLeaveDto.CreateRequest.builder()
                .startDate(start)
                .endDate(end)
                .type(LeaveType.ANNUAL)
                .reason("개인 사유")
                .build();
    }

    @Test
    @DisplayName("연차 신청 성공 - PENDING 상태로 저장된다")
    void submitLeave_success() {
        Long leaveId = annualLeaveService.submitLeave(
                employee.getId(), createRequest(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 5)));

        AnnualLeave saved = annualLeaveRepository.findById(leaveId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(saved.getUser().getId()).isEqualTo(employee.getId());
        assertThat(saved.getType()).isEqualTo(LeaveType.ANNUAL);
    }

    @Test
    @DisplayName("연차 신청 실패 - 시작일이 종료일보다 늦으면 예외가 발생한다")
    void submitLeave_fail_invalidPeriod() {
        assertThatThrownBy(() -> annualLeaveService.submitLeave(
                employee.getId(), createRequest(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 5))))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("시작일은 종료일보다 늦을 수 없습니다.");
    }

    @Test
    @DisplayName("연차 신청 실패 - 기존 신청 기간과 겹치면 예외가 발생한다")
    void submitLeave_fail_overlapping() {
        annualLeaveService.submitLeave(
                employee.getId(), createRequest(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 7)));

        assertThatThrownBy(() -> annualLeaveService.submitLeave(
                employee.getId(), createRequest(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 9))))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("이미 신청된 연차 기간과 겹칩니다.");
    }

    @Test
    @DisplayName("연차 신청 성공 - 반려된 기존 신청 기간과는 겹쳐도 허용된다")
    void submitLeave_success_overlapWithRejectedIsAllowed() {
        Long leaveId = annualLeaveService.submitLeave(
                employee.getId(), createRequest(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3)));
        AnnualLeave leave = annualLeaveRepository.findById(leaveId).orElseThrow();
        leave.reject(admin, "일정 조율 불가");
        em.flush();
        em.clear();

        Long newLeaveId = annualLeaveService.submitLeave(
                employee.getId(), createRequest(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3)));

        assertThat(newLeaveId).isNotNull();
    }

    @Test
    @DisplayName("연차 처리 실패 - 반려 처리 시 반려 사유가 없으면 예외가 발생한다")
    void processLeave_fail_rejectReasonRequired() {
        Long leaveId = annualLeaveService.submitLeave(
                employee.getId(), createRequest(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 5)));

        AnnualLeaveDto.ProcessRequest request = AnnualLeaveDto.ProcessRequest.builder()
                .status(LeaveStatus.REJECTED)
                .build();

        assertThatThrownBy(() -> annualLeaveService.processLeave(leaveId, admin.getId(), request))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("반려 사유를 입력해야 합니다.");
    }

    @Test
    @DisplayName("연차 처리 성공 - 반려 사유와 함께 반려 처리된다")
    void processLeave_success_reject() {
        Long leaveId = annualLeaveService.submitLeave(
                employee.getId(), createRequest(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 5)));

        AnnualLeaveDto.ProcessRequest request = AnnualLeaveDto.ProcessRequest.builder()
                .status(LeaveStatus.REJECTED)
                .rejectReason("업무 공백 우려")
                .build();

        AnnualLeaveDto.DetailResponse response = annualLeaveService.processLeave(leaveId, admin.getId(), request);

        assertThat(response.getStatus()).isEqualTo(LeaveStatus.REJECTED);
        assertThat(response.getRejectReason()).isEqualTo("업무 공백 우려");
    }

    @Test
    @DisplayName("연차 처리 성공 - 승인 처리된다")
    void processLeave_success_approve() {
        Long leaveId = annualLeaveService.submitLeave(
                employee.getId(), createRequest(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 5)));

        AnnualLeaveDto.ProcessRequest request = AnnualLeaveDto.ProcessRequest.builder()
                .status(LeaveStatus.APPROVED)
                .build();

        AnnualLeaveDto.DetailResponse response = annualLeaveService.processLeave(leaveId, admin.getId(), request);

        assertThat(response.getStatus()).isEqualTo(LeaveStatus.APPROVED);
    }

    @Test
    @DisplayName("연차 처리 실패 - 이미 처리된 신청은 다시 처리할 수 없다")
    void processLeave_fail_alreadyProcessed() {
        Long leaveId = annualLeaveService.submitLeave(
                employee.getId(), createRequest(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 5)));

        annualLeaveService.processLeave(leaveId, admin.getId(),
                AnnualLeaveDto.ProcessRequest.builder().status(LeaveStatus.APPROVED).build());

        assertThatThrownBy(() -> annualLeaveService.processLeave(leaveId, admin.getId(),
                AnnualLeaveDto.ProcessRequest.builder().status(LeaveStatus.APPROVED).build()))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("이미 처리된 연차 신청입니다.");
    }

    @Test
    @DisplayName("내 연차 신청 상세 조회 실패 - 타인의 신청 조회 시 예외가 발생한다")
    void getMyLeaveDetail_fail_forbidden() {
        Long leaveId = annualLeaveService.submitLeave(
                employee.getId(), createRequest(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 5)));

        assertThatThrownBy(() -> annualLeaveService.getMyLeaveDetail(admin.getId(), leaveId))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("본인의 연차 신청만 조회할 수 있습니다.");
    }

    @Test
    @DisplayName("연차 목록 조회 - 상태 필터로 PENDING 건만 조회된다")
    void getLeaveList_filterByStatus() {
        annualLeaveService.submitLeave(
                employee.getId(), createRequest(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 5)));
        Long approvedId = annualLeaveService.submitLeave(
                employee.getId(), createRequest(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 5)));
        annualLeaveService.processLeave(approvedId, admin.getId(),
                AnnualLeaveDto.ProcessRequest.builder().status(LeaveStatus.APPROVED).build());

        Page<AnnualLeaveDto.ListItem> pendingPage =
                annualLeaveService.getLeaveList(LeaveStatus.PENDING, PageRequest.of(0, 10));

        assertThat(pendingPage.getTotalElements()).isEqualTo(1);
        assertThat(pendingPage.getContent().get(0).getStatus()).isEqualTo(LeaveStatus.PENDING);
    }

    @Test
    @DisplayName("매장 연차 상세 조회 실패 - 타 매장 직원이면 CROSS_STORE_FORBIDDEN")
    void getLeaveDetailByStore_fail_crossStore() {
        // given
        Store otherStore = Store.builder()
                .name("다른매장")
                .address("서울시 마포구")
                .build();
        em.persist(otherStore);

        Long leaveId = annualLeaveService.submitLeave(
                employee.getId(), createRequest(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2)));

        // when & then
        assertThatThrownBy(() -> annualLeaveService.getLeaveDetailByStore(otherStore.getId(), leaveId))
                .isInstanceOf(InoutException.class)
                .satisfies(ex -> assertThat(((InoutException) ex).getResultCode()).isEqualTo("CROSS_STORE_FORBIDDEN"));
    }

    @Test
    @DisplayName("연차 처리 실패 - 처리자에 매장이 없으면 STORE_REQUIRED")
    void processLeave_fail_processorStoreRequired() {
        // given
        User hqAdmin = User.builder()
                .email("hq@test.com")
                .password("pw")
                .name("본사관리자")
                .phone("010-0000-0099")
                .birthday(LocalDate.of(1980, 1, 1))
                .deleted(false)
                .build();
        em.persist(hqAdmin);
        em.flush();

        Long leaveId = annualLeaveService.submitLeave(
                employee.getId(), createRequest(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 2)));

        // when & then
        assertThatThrownBy(() -> annualLeaveService.processLeave(leaveId, hqAdmin.getId(),
                AnnualLeaveDto.ProcessRequest.builder().status(LeaveStatus.APPROVED).build()))
                .isInstanceOf(InoutException.class)
                .satisfies(ex -> assertThat(((InoutException) ex).getResultCode()).isEqualTo("STORE_REQUIRED"));
    }
}
