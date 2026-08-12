package com.jstudy.inout.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.auth.entity.Role;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.entity.UserRole;
import com.jstudy.inout.order.dto.BulkOrderRequest;
import com.jstudy.inout.order.dto.BulkOrderResponse;
import com.jstudy.inout.order.dto.OrderAdminResponse;
import com.jstudy.inout.order.service.OrderAdmService;

@WebMvcTest(OrderAdmController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderAdmControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private OrderAdmService orderAdmService;

    private CustomUserDetails adminDetails;

    @BeforeEach
    void setUp() {
        User admin = User.builder()
                .id(99L)
                .email("admin@inout.com")
                .password("enc")
                .name("관리자")
                .phone("010-0000-0000")
                .birthday(LocalDate.of(1980, 1, 1))
                .deleted(false)
                .build();
        Role adminRole = Role.builder().roleId(1L).roleName("ROLE_ADMIN").build();
        admin.getUserRoles().add(UserRole.builder().user(admin).role(adminRole).build());
        adminDetails = new CustomUserDetails(admin);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminDetails, null, adminDetails.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("관리자 발주 목록 전체 조회 API")
    void getAllOrderRequests() throws Exception {
        OrderAdminResponse response = OrderAdminResponse.builder().orderRequestId(1L).storeName("본점").build();
        given(orderAdmService.getAllOrders(null)).willReturn(List.of(response));

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("발주 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.body[0].storeName").value("본점"));
    }

    @Test
    @DisplayName("발주 상세 항목 개별 승인/반려 처리 API")
    void processOrderDetail() throws Exception {
        String requestJson = "{\"items\": []}";

        mockMvc.perform(patch("/api/admin/orders/1/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("발주 상세 처리가 완료되었습니다."));
    }

    @Test
    @DisplayName("선택된 발주 건 일괄 승인 API")
    void bulkApprove() throws Exception {
        BulkOrderRequest request = new BulkOrderRequest();
        BulkOrderResponse response = BulkOrderResponse.builder()
                .successCount(3).autoRejectCount(1).failureCount(0).build();

        given(orderAdmService.bulkApproveOrders(any(), eq(99L))).willReturn(response);

        mockMvc.perform(post("/api/admin/orders/bulk-approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("3건 승인, 1건 재고부족 자동반려"))
                .andExpect(jsonPath("$.body.successCount").value(3))
                .andExpect(jsonPath("$.body.autoRejectCount").value(1));
    }

    @Test
    @DisplayName("엑셀 다운로드 API - 정상적으로 200 OK를 반환한다")
    void downloadExcel() throws Exception {
        mockMvc.perform(get("/api/admin/orders/excel"))
                .andExpect(status().isOk());
    }
}
