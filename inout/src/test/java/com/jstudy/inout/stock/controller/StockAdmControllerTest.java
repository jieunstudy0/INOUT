package com.jstudy.inout.stock.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.stock.dto.admin.StockAdminResponse;
import com.jstudy.inout.stock.dto.admin.StockRegister;
import com.jstudy.inout.stock.dto.admin.StockUpdate;
import com.jstudy.inout.stock.service.StockAdmService;

@WebMvcTest(StockAdmController.class)
@AutoConfigureMockMvc(addFilters = false) 
class StockAdmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StockAdmService stockAdmService;

    private UsernamePasswordAuthenticationToken mockPrincipal;

    @BeforeEach
    void setUp() {
        User adminUser = User.builder().id(1L).email("admin@test.com").name("관리자").build();
        CustomUserDetails userDetails = new CustomUserDetails(adminUser);
        mockPrincipal = new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(mockPrincipal);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("상품 등록 API - 정상 요청 시 200 OK와 생성된 ID 반환")
    void registerItem_Success() throws Exception {
        // given
        StockRegister request = StockRegister.builder()
                .name("모나미볼펜")
                .categoryId(1)
                .unitPrice(1000L)
                .minStockLevel(10)
                .build();

        given(stockAdmService.registerStock(any(StockRegister.class))).willReturn(100L);

        // when & then
        mockMvc.perform(post("/api/admin/stocks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("상품 등록이 완료되었습니다."))
                .andExpect(jsonPath("$.body").value(100));
    }

    @Test
    @DisplayName("상품 등록 API - 유효성 검사 실패 시 400 에러 발생 (단가 누락)")
    void registerItem_Fail_Validation() throws Exception {
        // given
        StockRegister request = StockRegister.builder()
                .name("모나미볼펜")
                .categoryId(1)
                .build();

        // when & then
        mockMvc.perform(post("/api/admin/stocks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); 
    }

    @Test
    @DisplayName("상품 수정 API - 관리자 ID와 함께 서비스 호출 성공")
    void updateItem_Success() throws Exception {
        // given
        StockUpdate request = StockUpdate.builder()
                .name("수정된볼펜")
                .categoryId(2)
                .unitPrice(1500L)
                .minStockLevel(20)
                .build();

        // when & then
        mockMvc.perform(put("/api/admin/stocks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("상품 정보가 수정되었습니다."))
                .andExpect(jsonPath("$.body").value(1));
    }

    @Test
    @DisplayName("상품 삭제 API - 삭제 요청 성공")
    void deleteItem_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/admin/stocks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("상품이 삭제되었습니다."))
                .andExpect(jsonPath("$.body").value(1));
    }

    @Test
    @DisplayName("재고 입고 API - 수량 입고 성공")
    void receiveStock_Success() throws Exception {
        // given 
        String requestJson = "{\"itemId\": 1, \"quantity\": 50, \"memo\": \"신규 입고\"}";

        given(stockAdmService.receiveStock(eq(1L), eq(50), eq(1L), eq("신규 입고"))).willReturn(1L);

        // when & then
        mockMvc.perform(post("/api/admin/stocks/receive")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("재고 입고가 완료되었습니다."))
                .andExpect(jsonPath("$.body").value(1));
    }

    @Test
    @DisplayName("관리자 재고 목록 조회 API - 페이징 처리되어 반환된다")
    void getAdminItemList_Success() throws Exception {
        // given
        StockAdminResponse item = StockAdminResponse.builder().itemId(1L).name("테스트상품").build();
        Page<StockAdminResponse> pageResult = new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);

        given(stockAdmService.getAdminStockList(any(), anyBoolean(), any())).willReturn(pageResult);

        // when & then
        mockMvc.perform(get("/api/admin/stocks")
                .param("name", "테스트")
                .param("deleted", "false")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("재고 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.body.content[0].name").value("테스트상품"));
    }
}