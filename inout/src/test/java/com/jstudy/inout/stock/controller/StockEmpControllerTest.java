package com.jstudy.inout.stock.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.stock.dto.emp.ItemResponse;
import com.jstudy.inout.stock.dto.emp.StockUserDetailResponse;
import com.jstudy.inout.stock.service.StockEmpService;

@WebMvcTest(StockEmpController.class)
@AutoConfigureMockMvc(addFilters = false) 
class StockEmpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockEmpService stockEmpService;

    private UsernamePasswordAuthenticationToken mockPrincipal;

    @BeforeEach
    void setUp() {
        User empUser = User.builder().id(2L).email("emp@test.com").name("직원").build();
        CustomUserDetails userDetails = new CustomUserDetails(empUser);
        mockPrincipal = new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(mockPrincipal);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("직원 재고 사용 API - 재고 사용 요청 성공")
    void useStock_Success() throws Exception {
        String requestJson = "{\"itemId\": 1, \"quantity\": 10, \"memo\": \"업무 사용\"}";

        given(stockEmpService.useStock(eq(1L), eq(10), eq(2L), eq("업무 사용"))).willReturn(1L);

        mockMvc.perform(post("/api/emp/stocks/use")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("재고 사용 처리가 완료되었습니다."))
                .andExpect(jsonPath("$.body").value(1));
    }

    @Test
    @DisplayName("직원 재고 사용 API - 수량이 1 미만일 경우 400 에러 발생")
    void useStock_Fail_Validation() throws Exception {
        String requestJson = "{\"itemId\": 1, \"quantity\": 0, \"memo\": \"잘못된 수량\"}";

        mockMvc.perform(post("/api/emp/stocks/use")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("직원용 상품 목록 조회 API - 페이징 처리되어 반환된다")
    void getEmployeeStockList_Success() throws Exception {
        ItemResponse item = ItemResponse.builder().itemId(1L).name("테스트상품").build();
        Page<ItemResponse> pageResult = new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);

        given(stockEmpService.getEmployeeStockList(any(), any())).willReturn(pageResult);

        mockMvc.perform(get("/api/emp/stocks")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("상품 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.body.content[0].name").value("테스트상품"));
    }

    @Test
    @DisplayName("직원용 상품 상세 조회 API - 상세 정보 반환")
    void getStockDetail_Success() throws Exception {
        StockUserDetailResponse detail = StockUserDetailResponse.builder()
                .itemId(1L)
                .name("테스트상품")
                .currentStock(50)
                .status("정상")
                .build();

        given(stockEmpService.getEmployeeStockDetail(1L)).willReturn(detail);

        mockMvc.perform(get("/api/emp/stocks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("재고 상세 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.body.name").value("테스트상품"))
                .andExpect(jsonPath("$.body.status").value("정상"));
    }
}
