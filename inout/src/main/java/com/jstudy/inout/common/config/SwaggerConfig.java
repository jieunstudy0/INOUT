package com.jstudy.inout.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI inoutOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("로컬 개발 서버")
                ))

                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("로그인 후 발급된 accessToken을 입력하세요. (Bearer 접두사 불필요)")
                        )
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("INOUT B2B 발주 시스템 API")
                .version("v1.0.0")
                .description("""
                        **INOUT** 은 본사-매장 간 B2B 발주·재고·배송·결제를 통합 관리하는 시스템입니다.
                        
                        ### 주요 도메인
                        | 도메인 | 설명 |
                        |--------|------|
                        | **Auth** | JWT 로그인, 토큰 갱신, 비밀번호 재설정 |
                        | **Order (직원)** | 장바구니 관리, 발주 신청·취소·이력 조회 |
                        | **Order (관리자)** | 발주 승인·반려·일괄 승인, 엑셀 다운로드 |
                        | **Stock (관리자)** | 상품 등록·수정·삭제, 입고·재고 조정, 저재고 알림 |
                        | **Stock (직원)** | 재고 사용, 목록·상세 조회 |
                        | **Payment** | 예치금 결제, 충전·환불 |
                        | **Delivery** | 배송 시작·완료 처리 |
                        | **Dashboard** | 실시간 현황 집계 |
                        
                        ### 인증 방식
                        `POST /api/user/login` 으로 발급된 **accessToken** 을 Authorize 버튼에 입력하면 \
                        이후 모든 API를 Swagger UI에서 직접 테스트할 수 있습니다.
                        """)
                .contact(new Contact()
                        .name("김지은")
                        .email("jieunstudy@kakao.com")
                );
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("1. 인증 (Auth)")
                .pathsToMatch("/api/user/**")
                .build();
    }

    @Bean
    public GroupedOpenApi employeeApi() {
        return GroupedOpenApi.builder()
                .group("2. 직원용 (Employee)")
                .pathsToMatch("/api/emp/**")
                .build();
    }

    @Bean
    public GroupedOpenApi paymentApi() {
        return GroupedOpenApi.builder()
                .group("3. 결제 (Payment)")
                .pathsToMatch("/api/payment/**", "/api/deposit/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("4. 관리자용 (Admin)")
                .pathsToMatch("/api/admin/**")
                .build();
    }

    @Bean
    public GroupedOpenApi dashboardApi() {
        return GroupedOpenApi.builder()
                .group("5. 대시보드 (Dashboard)")
                .pathsToMatch("/api/dashboard/**")
                .build();
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("0. 전체 API")
                .pathsToMatch("/api/**")
                .build();
    }
}
