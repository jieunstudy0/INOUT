package com.jstudy.inout.common.auth.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jstudy.inout.common.auth.service.AuthService;
import com.jstudy.inout.common.config.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminEmployeeController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminEmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("PATCH /api/admin/employees/{id}/unlock — 계정 잠금 해제")
    void unlockEmployee_success() throws Exception {
        mockMvc.perform(patch("/api/admin/employees/42/unlock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message")
                        .value("해당 사용자의 계정 잠금이 성공적으로 해제되었습니다."));

        verify(authService).unlockUser(42L);
    }
}
