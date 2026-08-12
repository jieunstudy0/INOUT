package com.jstudy.inout.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
import com.jstudy.inout.order.dto.OrderCreateRequest;
import com.jstudy.inout.order.dto.OrderPreResponse;
import com.jstudy.inout.order.service.OrderEmpService;

@WebMvcTest(OrderEmpController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderEmpControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private OrderEmpService orderEmpService;

    private CustomUserDetails employeeDetails;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(1L)
                .email("user@inout.com")
                .password("enc")
                .name("직원")
                .phone("010-1111-2222")
                .birthday(LocalDate.of(1995, 3, 3))
                .deleted(false)
                .build();
        Role empRole = Role.builder().roleId(2L).roleName("ROLE_EMPLOYEE").build();
        user.getUserRoles().add(UserRole.builder().user(user).role(empRole).build());
        employeeDetails = new CustomUserDetails(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(employeeDetails, null, employeeDetails.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("발주 미리보기 API - 장바구니 정보를 바탕으로 미리보기 응답 반환")
    void getOrderPreview() throws Exception {
        OrderCreateRequest request = OrderCreateRequest.builder().cartDetailIds(List.of(10L, 11L)).build();
        OrderPreResponse response = OrderPreResponse.builder()
                .employeeName("김직원")
                .totalPrice(30000L)
                .build();

        given(orderEmpService.getOrderPreview(eq(1L), any(OrderCreateRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/emp/orders/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("발주 미리보기를 불러왔습니다."))
                .andExpect(jsonPath("$.body.employeeName").value("김직원"))
                .andExpect(jsonPath("$.body.totalPrice").value(30000));
    }

    @Test
    @DisplayName("발주 최종 제출 API")
    void submitOrder() throws Exception {
        OrderCreateRequest request = OrderCreateRequest.builder().cartDetailIds(List.of(10L)).build();

        mockMvc.perform(post("/api/emp/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("주문서가 생성되었습니다."))
                .andExpect(jsonPath("$.body").isNumber());
    }

    @Test
    @DisplayName("발주 요청 취소 API")
    void cancelOrder() throws Exception {
        mockMvc.perform(patch("/api/emp/orders/100/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("발주가 정상적으로 취소되었습니다."))
                .andExpect(jsonPath("$.body").doesNotExist());
    }
}
