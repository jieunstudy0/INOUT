package com.jstudy.inout.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.dashboard.service.DashboardService;
import com.jstudy.inout.delivery.entity.Delivery;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import com.jstudy.inout.delivery.repository.DeliveryRepository;
import com.jstudy.inout.inquiry.entity.Inquiry;
import com.jstudy.inout.inquiry.repository.InquiryRepository;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCsService {

    private final InquiryRepository inquiryRepository;
    private final OrderRequestRepository orderRequestRepository;
    private final DeliveryRepository deliveryRepository;
    private final DashboardService dashboardService;
    private final AiCsPersistenceService persistenceService;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int BATCH_SIZE = 10;
    private static final int RECENT_ORDER_LIMIT = 3;
    private static final Set<String> VALID_CATEGORIES = Set.of("배송", "교환/환불", "기타");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private record AiCsAnalysis(String category, String draftAnswer) {}

    private final RestClient geminiRestClient = buildGeminiRestClient();

    private static RestClient buildGeminiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return RestClient.builder().requestFactory(factory).build();
    }

    public int processWaitingInquiries() {
        if (!StringUtils.hasText(apiKey)) {
            throw new InoutException(
                    "Gemini API 키가 설정되지 않았습니다. application-secret.properties에 gemini.api-key를 설정해 주세요.",
                    503, "GEMINI_NOT_CONFIGURED");
        }

        List<Inquiry> targets = inquiryRepository
                .findByIsReadFalseAndAiDraftAnswerIsNullOrderByCreatedAtAsc(PageRequest.of(0, BATCH_SIZE));

        if (targets.isEmpty()) {
            log.info("[AI CS 자동화] 처리 대상 문의 없음. 건너뜀.");
            return 0;
        }
        log.info("[AI CS 자동화] 처리 대상 문의 {}건 감지. Gemini 분석 시작...", targets.size());

        int processedCount = 0;
        for (Inquiry inquiry : targets) {
            try {
                List<OrderRequest> recentOrders = orderRequestRepository
                        .findAllByRequestUser_IdOrderByRequestDateDesc(inquiry.getAuthor().getId())
                        .stream()
                        .limit(RECENT_ORDER_LIMIT)
                        .toList();

                String userPrompt = buildPrompt(inquiry, recentOrders);
                String rawResponse = callGeminiForJsonObject(userPrompt);
                AiCsAnalysis analysis = extractAndParseAnalysis(rawResponse);

                String category = VALID_CATEGORIES.contains(analysis.category()) ? analysis.category() : "기타";
                if (persistenceService.updateInquiryAnalysis(inquiry.getId(), category, analysis.draftAnswer())) {
                    processedCount++;
                }

            } catch (Exception e) {
                log.warn("[AI CS 자동화] inquiryId={} 분석 실패. 건너뜀. 사유: {}", inquiry.getId(), e.getMessage());
            }
        }

        if (processedCount > 0) {
            try {
                dashboardService.evictDashboardSummary();
            } catch (RuntimeException ex) {
                log.warn("[AI CS 자동화] 대시보드 캐시 무효화 실패(무시). Redis 미가용해도 CS 초안은 유지됩니다. cause={}",
                        ex.getMessage());
            }
        }
        log.info("[AI CS 자동화] 총 {}건 중 {}건 분석 완료", targets.size(), processedCount);
        return processedCount;
    }

    
    private String buildPrompt(Inquiry inquiry, List<OrderRequest> recentOrders) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 고객 문의 내용\n");
        sb.append("- 제목: ").append(inquiry.getTitle()).append('\n');
        sb.append("- 작성자: ").append(inquiry.getAuthor().getName()).append('\n');
        sb.append("- 본문: ").append(inquiry.getContent()).append("\n\n");

        sb.append("## 이 고객의 최근 주문 내역 (최대 ").append(RECENT_ORDER_LIMIT).append("건)\n");
        if (recentOrders.isEmpty()) {
            sb.append("- 최근 주문 내역 없음\n");
        } else {
            for (OrderRequest order : recentOrders) {
                String deliveryDesc = deliveryRepository.findByOrderRequest_Id(order.getId())
                        .map(Delivery::getStatus)
                        .map(DeliveryStatus::getDescription)
                        .orElse("배송 정보 없음");
                sb.append(String.format("- 주문 #%d (%s): 주문상태=%s, 배송상태=%s, 금액=%,d원%n",
                        order.getId(),
                        order.getRequestDate() != null ? order.getRequestDate().format(DATE_FMT) : "-",
                        order.getStatus() != null ? order.getStatus().getDescription() : "알수없음",
                        deliveryDesc,
                        order.getTotalPrice() != null ? order.getTotalPrice() : 0L));
            }
        }
        return sb.toString();
    }

    private String callGeminiForJsonObject(String userPrompt) {
        String systemInstruction =
                "당신은 B2B 유통 플랫폼의 CS(고객 문의) 상담 전문가입니다. " +
                "제공된 고객 문의 내용과 최근 주문/배송 내역을 참고하여 다음 두 가지를 수행하세요.\n" +
                "1) 문의 카테고리를 반드시 '배송', '교환/환불', '기타' 중 하나로만 분류하세요.\n" +
                "2) 담당자가 검토 후 그대로 복사해서 답장할 수 있도록, 친절하고 정중한 한국어 답변 초안을 작성하세요. " +
                "필요하다면 최근 주문/배송 상태를 답변에 자연스럽게 반영하세요.\n" +
                "반드시 다른 자연어 설명 없이 아래 형식의 순수 JSON 객체만 반환하세요. " +
                "마크다운 코드블록(```json)을 사용하지 마세요. 오직 JSON 텍스트만 출력하세요.\n" +
                "형식: {\"category\": \"배송\", \"draftAnswer\": \"안녕하세요 고객님, ...\"}";

        Map<String, Object> requestBody = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                "contents", List.of(Map.of("parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig", Map.of("temperature", 0.3, "maxOutputTokens", 1024)
        );

        String url = GEMINI_BASE_URL + geminiModel + ":generateContent?key=" + apiKey;

        try {
            String responseBody = geminiRestClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty()) {
                throw new InoutException("Gemini 응답에 candidates 필드가 없습니다.", 500, "AI_INVALID_RESPONSE");
            }
            return candidates.get(0).path("content").path("parts").get(0).path("text").asText();

        } catch (RestClientException e) {
            log.error("[AI CS 자동화] Gemini API 호출 실패", e);
            throw new InoutException("Gemini API 서비스 연결에 실패했습니다.", 503, "AI_API_ERROR");
        } catch (InoutException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AI CS 자동화] Gemini 응답 파싱 실패", e);
            throw new InoutException("Gemini 응답 처리 중 오류가 발생했습니다.", 500, "AI_PARSE_ERROR");
        }
    }


    private AiCsAnalysis extractAndParseAnalysis(String rawText) {
        String cleaned = rawText
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start == -1 || end == -1 || start > end) {
            log.error("[AI CS 자동화] JSON 객체 추출 실패. 원본: {}", rawText);
            throw new InoutException(
                    "AI 응답에서 JSON 객체를 찾을 수 없습니다. AI가 잘못된 형식을 반환했습니다.",
                    500, "AI_PARSE_ERROR");
        }
        String jsonObject = cleaned.substring(start, end + 1);

        try {
            return objectMapper.readValue(jsonObject, AiCsAnalysis.class);
        } catch (Exception e) {
            log.error("[AI CS 자동화] JSON 역직렬화 실패. jsonObject={}", jsonObject, e);
            throw new InoutException("AI 분석 데이터 파싱에 실패했습니다.", 500, "AI_DESERIALIZE_ERROR");
        }
    }
}
