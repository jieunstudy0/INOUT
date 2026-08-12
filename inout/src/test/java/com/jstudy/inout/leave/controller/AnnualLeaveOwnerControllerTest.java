package com.jstudy.inout.leave.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.config.handler.GlobalExceptionHandler;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.leave.dto.AnnualLeaveDto;
import com.jstudy.inout.leave.entity.LeaveStatus;
import com.jstudy.inout.leave.service.AnnualLeaveService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnnualLeaveOwnerController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AnnualLeaveOwnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnnualLeaveService annualLeaveService;

    @BeforeEach
    void setUp() {
        Store store = Store.builder().id(10L).name("지점 1호").address("서울").build();
        User owner = User.builder().id(5L).email("owner1@test.com").name("점주1").store(store).build();
        CustomUserDetails details = new CustomUserDetails(owner);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, "", details.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("점주 연차 목록 조회 성공")
    void getLeaveList_success() throws Exception {
        // given
        AnnualLeaveDto.ListItem item = AnnualLeaveDto.ListItem.builder()
                .leaveId(1L)
                .employeeName("직원1")
                .status(LeaveStatus.PENDING)
                .build();
        given(annualLeaveService.getLeaveListByStore(eq(10L), isNull(), any()))
                .willReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1));

        // when & then
        mockMvc.perform(get("/api/owner/vacation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.content[0].employeeName").value("직원1"));
    }

    @Test
    @DisplayName("점주 연차 처리 실패 - 타 매장 접근 시 CROSS_STORE_FORBIDDEN(403)")
    void processLeave_fail_crossStore() throws Exception {
        // given
        given(annualLeaveService.processLeave(eq(99L), eq(5L), any()))
                .willThrow(new InoutException("다른 매장 직원의 연차에 접근할 수 없습니다.", 403, "CROSS_STORE_FORBIDDEN"));

        AnnualLeaveDto.ProcessRequest request = AnnualLeaveDto.ProcessRequest.builder()
                .status(LeaveStatus.APPROVED)
                .build();

        // when & then
        mockMvc.perform(patch("/api/owner/vacation/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.header.resultCode").value("CROSS_STORE_FORBIDDEN"));
    }

    @Test
    @DisplayName("점주 연차 처리 실패 - ProcessRequest status 누락 시 400")
    void processLeave_fail_validation() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/owner/vacation/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
