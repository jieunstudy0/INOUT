package com.jstudy.inout.common.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
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
    @DisplayName("ACL - URL 규칙은 통과하고 @PreAuthorize에서 거부되면 500이 아닌 403 AUTH_403")
    @WithMockUser(roles = "ADMIN")
    void methodSecurityDenialReturns403() throws Exception {
        // /api/emp/** 는 ADMIN 허용, 메서드는 EMPLOYEE 전용
        mockMvc.perform(get("/api/emp/inquiries/to-admin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.header.resultCode").value("AUTH_403"));
    }

    @Test
    @DisplayName("ACL - ADMIN이 문의글 작성 시 메서드 보안 거부 403 AUTH_403")
    @WithMockUser(roles = "ADMIN")
    void adminCannotCreateInquiry() throws Exception {
        // 바인딩·@Valid 검증이 권한 검사보다 먼저 일어나므로, 400에 걸리지 않게 유효한 값을 넣는다
        mockMvc.perform(multipart("/api/inquiry")
                        .param("title", "제목")
                        .param("content", "내용"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.header.resultCode").value("AUTH_403"));
    }

    @Test
    @DisplayName("ACL - OWNER가 문의 댓글 작성 시 메서드 보안 거부 403 AUTH_403")
    @WithMockUser(roles = "OWNER")
    void ownerCannotCreateInquiryComment() throws Exception {
        mockMvc.perform(post("/api/inquiry/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"test\"}"))
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
