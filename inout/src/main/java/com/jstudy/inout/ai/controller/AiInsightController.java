package com.jstudy.inout.ai.controller;

import com.jstudy.inout.ai.dto.AiInsightResponse;
import com.jstudy.inout.ai.service.AiAutoOrderService;
import com.jstudy.inout.ai.service.AiCsService;
import com.jstudy.inout.ai.service.AiInsightService;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.common.exception.InoutException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Tag(name = "AI 인사이트", description = "Gemini 기반 발주/재고 운영 분석 리포트, 자동 발주 초안 생성, 고객 문의(CS) 자동 분류·답변 초안 생성")
@Slf4j
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AiInsightController {

    private final AiInsightService aiInsightService;
    private final AiAutoOrderService aiAutoOrderService;
    private final AiCsService aiCsService;

    private static final int TIMEOUT_SECONDS = 35;

    @Operation(
            summary = "AI 운영 인사이트 리포트 생성",
            description = "최근 6개월 발주 트렌드 · 재고 현황 · 매장별 발주 빈도를 Gemini 2.5 Flash 모델에 전달하여 " +
                          "자연어 인사이트 리포트를 생성합니다. 응답까지 최대 35초가 소요될 수 있습니다.")
    @ApiResponse(responseCode = "200", description = "리포트 생성 성공")
    @ApiResponse(responseCode = "503", description = "API 키 미설정 또는 OpenAI 서비스 오류")
    @ApiResponse(responseCode = "504", description = "응답 시간 초과 (35초)")
    @GetMapping("/insight")
    public ResponseEntity<?> getInsightReport() {
        log.info("AI 인사이트 리포트 요청 수신");

        CompletableFuture<AiInsightResponse> future = aiInsightService.generateInsightReport();

        try {
            AiInsightResponse response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return ResponseResult.successWithData(response);

        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("AI 인사이트 요청 타임아웃 ({}초 초과)", TIMEOUT_SECONDS);
            throw new InoutException(
                    "AI 분석 요청이 시간 초과되었습니다 (" + TIMEOUT_SECONDS + "초). 잠시 후 다시 시도해 주세요.",
                    504, "AI_TIMEOUT");

        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InoutException ie) throw ie;
            log.error("AI 인사이트 비동기 실행 오류", cause);
            throw new InoutException("AI 분석 처리 중 내부 오류가 발생했습니다.", 500, "AI_EXECUTION_ERROR");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InoutException("AI 분석 요청이 중단되었습니다.", 500, "AI_INTERRUPTED");
        }
    }

    @Operation(
            summary = "[수동 트리거] AI 지능형 재고 분석 및 자동 발주 제안 생성",
            description = "전체 활성 상품의 현재 재고·안전 재고·최근 7일 판매 속도(Sales Velocity)를 Gemini에게 분석 요청하여 " +
                          "재고 보충이 시급한 상품에 대한 AI 제안 발주 초안(isAiSuggested=true, WAITING 상태)을 생성합니다. " +
                          "매일 자정 스케줄러가 자동 실행하지만, 이 API로 즉시 수동 실행할 수 있습니다. " +
                          "성공 시 대시보드의 'AI 스마트 발주 제안' 통계가 즉시 갱신됩니다.")
    @ApiResponse(responseCode = "200", description = "분석 완료 (생성 건수 반환)")
    @ApiResponse(responseCode = "503", description = "Gemini API 키 미설정")
    @PostMapping("/auto-order-analyze")
    public ResponseEntity<?> triggerAutoOrderDraft() {
        log.info("[AI 자동 발주] 수동 트리거 요청 수신");
        int savedCount = aiAutoOrderService.createAutoOrderDraft();
        String message = savedCount == 0
                ? "재고 보충이 필요한 상품이 없거나 AI 추천 결과가 없어 발주 초안이 생성되지 않았습니다."
                : "1건의 통합 AI 제안 발주 초안이 생성되었습니다. 발주 관리 목록에서 확인하세요.";
        return ResponseResult.successWithData(Map.of("savedCount", savedCount, "message", message));
    }

    @Operation(
            summary = "[수동 트리거] AI 고객 문의(CS) 자동 분류 및 답변 초안 생성 (일괄)",
            description = "AI 초안이 아직 없는 문의를 최대 10건 조회하여 Gemini에게 " +
                          "카테고리 분류와 답변 초안 작성을 요청합니다. 10분마다 스케줄러가 자동 실행하지만, " +
                          "이 API로 즉시 수동 실행할 수 있습니다.")
    @ApiResponse(responseCode = "200", description = "AI 분석 완료 (처리 건수 반환)")
    @ApiResponse(responseCode = "503", description = "Gemini API 키 미설정")
    @PostMapping("/cs-classify")
    public ResponseEntity<?> triggerCsClassification() {
        log.info("[AI CS 자동화] 수동 트리거 요청 수신");
        int processedCount = aiCsService.processWaitingInquiries();
        String message = processedCount == 0
                ? "처리할 답변 대기 문의가 없습니다."
                : processedCount + "건의 문의에 대해 AI 분류 및 답변 초안 생성이 완료되었습니다.";
        return ResponseResult.successWithData(Map.of("processedCount", processedCount, "message", message));
    }

    @Operation(
            summary = "[수동 트리거] 특정 문의 AI 분류 및 답변 초안 생성 (단건)",
            description = "지정한 문의 ID에 대해 Gemini가 카테고리 분류와 답변 초안을 생성합니다. " +
                          "이미 AI 초안이 있는 문의는 409를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "AI 초안 생성 완료")
    @ApiResponse(responseCode = "404", description = "문의 없음")
    @ApiResponse(responseCode = "409", description = "이미 AI 초안이 존재함")
    @ApiResponse(responseCode = "503", description = "Gemini API 키 미설정")
    @PostMapping("/cs-classify/{inquiryId}")
    public ResponseEntity<?> triggerCsClassificationForOne(@PathVariable("inquiryId") Long inquiryId) {
        log.info("[AI CS 자동화] 단건 트리거 요청 수신 — inquiryId={}", inquiryId);
        aiCsService.processSingleInquiry(inquiryId);
        return ResponseResult.successWithData(Map.of(
                "inquiryId", inquiryId,
                "message", "해당 문의에 대한 AI 분류 및 답변 초안 생성이 완료되었습니다."));
    }
}
