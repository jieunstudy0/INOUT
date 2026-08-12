package com.jstudy.inout.common.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jstudy.inout.common.mail.config.MailComponent;
import com.jstudy.inout.delivery.service.DeliveryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityPathAclSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MailComponent mailComponent;

    @MockBean
    private DeliveryService deliveryService;

    @Test
    @DisplayName("ACL - EMPLOYEE가 OWNER 배송 API 호출 시 403 AUTH_403")
    @WithMockUser(roles = "EMPLOYEE")
    void employeeCannotAccessOwnerDeliveries() throws Exception {
        mockMvc.perform(get("/api/owner/deliveries"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.header.resultCode").value("AUTH_403"));
    }

    @Test
    @DisplayName("ACL - EMPLOYEE가 OWNER 대시보드 API 호출 시 403 AUTH_403")
    @WithMockUser(roles = "EMPLOYEE")
    void employeeCannotAccessOwnerDashboard() throws Exception {
        mockMvc.perform(get("/api/owner/dashboard/summary"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.header.resultCode").value("AUTH_403"));
    }

    @Test
    @DisplayName("ACL - OWNER가 ADMIN 배송 관리 API 호출 시 403 AUTH_403")
    @WithMockUser(roles = "OWNER")
    void ownerCannotAccessAdminDeliveries() throws Exception {
        mockMvc.perform(get("/api/admin/deliveries"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.header.resultCode").value("AUTH_403"));
    }

    @Test
    @DisplayName("ACL - EMPLOYEE가 ADMIN 연차 모니터링 API 호출 시 403 AUTH_403")
    @WithMockUser(roles = "EMPLOYEE")
    void employeeCannotAccessAdminVacation() throws Exception {
        mockMvc.perform(get("/api/admin/vacation"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.header.resultCode").value("AUTH_403"));
    }

    @Test
    @DisplayName("ACL - 미인증 사용자가 OWNER API 호출 시 401")
    void anonymousCannotAccessOwnerApi() throws Exception {
        mockMvc.perform(get("/api/owner/users"))
                .andExpect(status().isUnauthorized());
    }
}
