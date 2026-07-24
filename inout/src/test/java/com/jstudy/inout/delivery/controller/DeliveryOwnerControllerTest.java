package com.jstudy.inout.delivery.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.config.handler.GlobalExceptionHandler;
import com.jstudy.inout.delivery.dto.DeliveryDto;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import com.jstudy.inout.delivery.service.DeliveryService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DeliveryOwnerController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class DeliveryOwnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeliveryService deliveryService;

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
    @DisplayName("점주 매장 배송 목록 조회 성공 - store 스코프로 페이징 결과를 반환한다")
    void getStoreDeliveryList_success() throws Exception {
        // given
        DeliveryDto.ListItem item = DeliveryDto.ListItem.builder()
                .deliveryId(1L)
                .orderId(100L)
                .status(DeliveryStatus.SHIPPING)
                .receiverName("홍길동")
                .build();
        given(deliveryService.getStoreDeliveryList(eq(10L), isNull(), any()))
                .willReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1));

        // when & then
        mockMvc.perform(get("/api/owner/deliveries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("매장 배송 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.body.content[0].orderId").value(100));
    }

    @Test
    @DisplayName("점주 매장 배송 목록 조회 실패 - 소속 매장이 없으면 STORE_REQUIRED(403)")
    void getStoreDeliveryList_fail_storeRequired() throws Exception {
        // given
        User ownerNoStore = User.builder().id(5L).email("owner1@test.com").name("점주1").build();
        CustomUserDetails details = new CustomUserDetails(ownerNoStore);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, "", details.getAuthorities()));

        // when & then
        mockMvc.perform(get("/api/owner/deliveries"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.header.resultCode").value("STORE_REQUIRED"));
    }
}
