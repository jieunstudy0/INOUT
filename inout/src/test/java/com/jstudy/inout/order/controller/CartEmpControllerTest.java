package com.jstudy.inout.order.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.jstudy.inout.order.dto.CartAddRequest;
import com.jstudy.inout.order.dto.CartResponse;
import com.jstudy.inout.order.service.CartEmpService;

@WebMvcTest(CartEmpController.class)
@AutoConfigureMockMvc(addFilters = false)
class CartEmpControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CartEmpService cartEmpService;

    private CustomUserDetails employeeDetails;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(1L)
                .email("user@inout.com")
                .password("enc")
                .name("직원")
                .phone("010-2222-3333")
                .birthday(LocalDate.of(1994, 4, 4))
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
    @DisplayName("장바구니 추가 API")
    void addToCart() throws Exception {
        CartAddRequest request = new CartAddRequest(100L, 2);

        mockMvc.perform(post("/api/emp/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("상품이 장바구니에 담겼습니다."))
                .andExpect(jsonPath("$.body").value(nullValue()));
    }

    @Test
    @DisplayName("장바구니 목록 조회 API")
    void getCartList() throws Exception {
        CartResponse response = CartResponse.builder().totalQuantity(5).totalPrice(50000L).build();
        given(cartEmpService.getCartList(1L)).willReturn(response);

        mockMvc.perform(get("/api/emp/carts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("장바구니 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.body.totalPrice").value(50000));
    }

    @Test
    @DisplayName("장바구니 선택 항목 삭제 API")
    void deleteSelectedItems() throws Exception {
        List<Long> cartDetailIds = List.of(10L, 11L);

        mockMvc.perform(delete("/api/emp/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartDetailIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("선택한 상품이 삭제되었습니다."))
                .andExpect(jsonPath("$.body").value(nullValue()));
    }

    @Test
    @DisplayName("장바구니 전체 비우기 API")
    void deleteAllItems() throws Exception {
        mockMvc.perform(delete("/api/emp/carts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("장바구니가 비워졌습니다."))
                .andExpect(jsonPath("$.body").value(nullValue()));
    }

    @Test
    @DisplayName("과거 발주 내역 재주문 API")
    void reOrder() throws Exception {
        mockMvc.perform(post("/api/emp/carts/reorder/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("과거 주문 상품이 장바구니에 담겼습니다."))
                .andExpect(jsonPath("$.body").value(nullValue()));
    }
}
