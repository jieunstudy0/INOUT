package com.jstudy.inout.delivery.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.config.handler.GlobalExceptionHandler;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.delivery.dto.DeliveryDto;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import com.jstudy.inout.delivery.service.DeliveryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DeliveryAdmController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class DeliveryAdmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeliveryService deliveryService;

    private UsernamePasswordAuthenticationToken mockPrincipal;

    @BeforeEach
    void setUp() {
        User admin = User.builder().id(99L).email("admin@inout.com").name("관리자").build();
        CustomUserDetails userDetails = new CustomUserDetails(admin);
        mockPrincipal = new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(mockPrincipal);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("배송 상세 조회 API - 주문 ID 기준 배송 정보 반환")
    void getDelivery_Success() throws Exception {
        // given
        DeliveryDto.DetailResponse response = DeliveryDto.DetailResponse.builder()
                .deliveryId(1L)
                .orderId(100L)
                .status(DeliveryStatus.READY)
                .receiverName("홍길동")
                .build();
        given(deliveryService.getDeliveryByOrderId(100L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/admin/deliveries/orders/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("배송 정보 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.body.orderId").value(100L))
                .andExpect(jsonPath("$.body.receiverName").value("홍길동"));
    }

    @Test
    @DisplayName("배송 시작 API - 운송장 번호 등록 후 배송 중 상태로 변경")
    void startShipping_Success() throws Exception {
        // given
        DeliveryDto.DetailResponse response = DeliveryDto.DetailResponse.builder()
                .deliveryId(1L)
                .orderId(100L)
                .status(DeliveryStatus.SHIPPING)
                .trackingNumber("TRK-1234")
                .build();
        given(deliveryService.startShipping(eq(100L), any(DeliveryDto.StartShippingRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/admin/deliveries/orders/100/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"trackingNumber\":\"TRK-1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("배송 시작 처리가 완료되었습니다."))
                .andExpect(jsonPath("$.body.status").value("SHIPPING"))
                .andExpect(jsonPath("$.body.trackingNumber").value("TRK-1234"));
    }

    @Test
    @DisplayName("배송 완료 API - 배송 중 건을 완료 상태로 변경")
    void completeDelivery_Success() throws Exception {
        // given
        DeliveryDto.DetailResponse response = DeliveryDto.DetailResponse.builder()
                .deliveryId(1L)
                .orderId(100L)
                .status(DeliveryStatus.COMPLETED)
                .build();
        given(deliveryService.completeDelivery(eq(100L), any())).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/admin/deliveries/orders/100/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("배송 완료 처리가 완료되었습니다."))
                .andExpect(jsonPath("$.body.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("배송 시작 API 실패 - 서비스에서 InoutException 발생 시 fail 응답 반환")
    void startShipping_Fail_InoutException() throws Exception {
        // given
        given(deliveryService.startShipping(eq(100L), any(DeliveryDto.StartShippingRequest.class)))
                .willThrow(new InoutException("관리자만 배송 상태를 변경할 수 있습니다.", 403, "FORBIDDEN"));

        DeliveryDto.StartShippingRequest request = DeliveryDto.StartShippingRequest.builder()
                .trackingNumber("TRK-0000")
                .build();

        // when & then
        mockMvc.perform(patch("/api/admin/deliveries/orders/100/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.header.result").value(false))
                .andExpect(jsonPath("$.header.resultCode").value("FORBIDDEN"))
                .andExpect(jsonPath("$.header.message").value("관리자만 배송 상태를 변경할 수 있습니다."));
    }
}
