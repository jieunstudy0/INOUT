package com.jstudy.inout.common.massdata;

import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.common.massdata.dto.MassDataGenerationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@Tag(name = "대량 더미 데이터 생성", description = "JdbcTemplate.batchUpdate() 기반 초고속 벌크 인서트로 전체 도메인 더미 데이터를 생성한다 (관리자 전용, local/dev 전용)")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class MassDataController {

    private final MassDataGeneratorService massDataGeneratorService;

    @Operation(
            summary = "대량 더미 데이터 생성 (Bulk Insert)",
            description = "scale=1 기준 회원/카테고리/상품 각 100건, 주문/문의 등 트랜잭션 데이터 300건을 기본으로 하며, " +
                    "scale 배수만큼 곱해 전체 도메인 엔티티(Role~ChargeRequest)에 걸쳐 FK 순서를 지켜 벌크 인서트한다. " +
                    "AI 재고 분석 테스트를 위해 '인기 상품' 몇 개의 재고를 낮게 설정하고 최근 7일 판매 이력을 몰아주며, " +
                    "AI CS 자동화 테스트를 위해 문의의 70%를 답변 대기(WAITING) 상태로 생성한다. " +
                    "OOM 방지를 위해 scale은 1~20 사이만 허용한다.")
    @ApiResponse(responseCode = "200", description = "생성 완료 (엔티티별 삽입 건수 및 소요 시간 반환)")
    @ApiResponse(responseCode = "400", description = "scale 값이 허용 범위(1~20)를 벗어남")
    @PostMapping("/generate-dummy")
    public ResponseEntity<?> generateDummyData(
            @Parameter(description = "기본 생성 건수에 곱할 배율 (1~20)", example = "1")
            @RequestParam(defaultValue = "1") int scale) {
        log.info("[대량 더미 데이터] 생성 요청 수신 (scale={})", scale);
        MassDataGenerationResponse response = massDataGeneratorService.generate(scale);
        return ResponseResult.successWithData(response);
    }
}
