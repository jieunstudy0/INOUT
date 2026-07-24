package com.jstudy.inout.dashboard.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.config.handler.GlobalExceptionHandler;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.dashboard.dto.DashboardOwnerResponse;
import com.jstudy.inout.dashboard.service.DashboardOwnerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardOwnerController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardOwnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardOwnerService dashboardOwnerService;

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
    @DisplayName("점주 대시보드 요약 조회 성공 - 매장 KPI를 반환한다")
    void getSummary_success() throws Exception {
        // given
        given(dashboardOwnerService.getSummary(5L)).willReturn(DashboardOwnerResponse.builder()
                .ownerName("점주1")
                .storeName("지점 1호")
                .storeId(10L)
                .todayOrderCount(3)
                .shippingDeliveryCount(2)
                .pendingLeaveCount(1)
                .build());

        // when & then
        mockMvc.perform(get("/api/owner/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.storeName").value("지점 1호"))
                .andExpect(jsonPath("$.body.todayOrderCount").value(3))
                .andExpect(jsonPath("$.body.shippingDeliveryCount").value(2));
    }

    @Test
    @DisplayName("점주 대시보드 조회 실패 - 서비스가 STORE_REQUIRED를 던지면 403을 반환한다")
    void getSummary_fail_storeRequired() throws Exception {
        // given
        given(dashboardOwnerService.getSummary(5L))
                .willThrow(new InoutException("소속 매장 정보가 없습니다.", 403, "STORE_REQUIRED"));

        // when & then
        mockMvc.perform(get("/api/owner/dashboard/summary"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.header.resultCode").value("STORE_REQUIRED"));
    }
}
