package com.jstudy.inout.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockUsageHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAutoOrderService {

    private final ItemRepository itemRepository;
    private final StockUsageHistoryRepository usageHistoryRepository;
    private final AiAutoOrderPersistenceService persistenceService;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String apiKey;

    /** application.properties 에서 모델명을 주입. 기본값 = gemini-1.5-flash (트래픽 안정성 우선). */
    @Value("${gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int SALES_VELOCITY_DAYS = 7;
    private static final int MAX_ITEMS_PER_ANALYSIS = 300;
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    /** 입고 리드타임 가정 (일). 발주 권장 수량 산정에 사용. */
    private static final int LEAD_TIME_DAYS = 7;

    /** 503 / 429 발생 시 최대 재시도 횟수(첫 시도 포함). 2 = 최초 1회 + 재시도 1회. */
    private static final int MAX_RETRY_ATTEMPTS = 2;
    /** 재시도 기본 대기 시간(ms). attempt 번호에 비례해 지수 백오프 적용. */
    private static final long RETRY_BASE_BACKOFF_MS = 1_500L;

    private final RestClient geminiRestClient = buildGeminiRestClient();

    private static RestClient buildGeminiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return RestClient.builder().requestFactory(factory).build();
    }

    record AiOrderRecommendation(Long itemId, boolean needsReorder, int recommendQty, String reason) {}

    // ──────────────────────────────────────────────────────────────────
    // Public entry point
    // ──────────────────────────────────────────────────────────────────

    public int createAutoOrderDraft() {
        if (!StringUtils.hasText(apiKey)) {
            throw new InoutException(
                    "Gemini API 키가 설정되지 않았습니다. application-secret.properties에 gemini.api-key를 설정해 주세요.",
                    503, "GEMINI_NOT_CONFIGURED");
        }

        List<Item> activeItems = itemRepository.findAllByDeletedFalse();
        if (activeItems.isEmpty()) {
            log.info("[AI 자동 발주] 분석 대상 활성 상품 없음. 발주 초안 생성 건너뜀.");
            return 0;
        }
        if (activeItems.size() > MAX_ITEMS_PER_ANALYSIS) {
            log.warn("[AI 자동 발주] 활성 상품 수({})가 1회 분석 한도({})를 초과하여 앞쪽 {}개만 분석합니다.",
                    activeItems.size(), MAX_ITEMS_PER_ANALYSIS, MAX_ITEMS_PER_ANALYSIS);
            activeItems = activeItems.subList(0, MAX_ITEMS_PER_ANALYSIS);
        }

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(SALES_VELOCITY_DAYS);
        Map<Long, Long> salesVelocityMap = new HashMap<>();
        for (Object[] row : usageHistoryRepository.sumRecentSalesByItem(sevenDaysAgo)) {
            salesVelocityMap.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        log.info("[AI 자동 발주] 활성 상품 {}개, 최근 {}일 판매 이력 보유 상품 {}개. Gemini 프롬프트 구성 중...",
                activeItems.size(), SALES_VELOCITY_DAYS, salesVelocityMap.size());

        // ── AI 호출 — 실패 시 규칙 기반 폴백 ────────────────────────────
        List<AiOrderRecommendation> recommendations;
        try {
            String userPrompt = buildPrompt(activeItems, salesVelocityMap);
            String rawResponse = callGeminiForJsonArray(userPrompt);
            recommendations = extractAndParseRecommendations(rawResponse).stream()
                    .filter(AiOrderRecommendation::needsReorder)
                    .toList();
            log.info("[AI 자동 발주] Gemini 추천 결과 {}건 수신.", recommendations.size());

        } catch (Exception e) {
            log.warn("[AI 자동 발주] Gemini 일시 장애로 규칙 기반 발주 제안으로 대체합니다. cause={}", e.getMessage());
            recommendations = buildRuleBasedRecommendations(activeItems, salesVelocityMap);
            log.info("[AI 자동 발주] 규칙 기반 대체 추천 {}건 산출 완료.", recommendations.size());
        }

        if (recommendations.isEmpty()) {
            log.info("[AI 자동 발주] 재고 보충이 시급한 상품 없음. 발주 초안 생성 건너뜀.");
            return 0;
        }

        Map<Long, Item> itemMap = activeItems.stream()
                .collect(Collectors.toMap(Item::getItemId, item -> item));
        return persistenceService.saveDraftOrder(itemMap, recommendations);
    }

    // ──────────────────────────────────────────────────────────────────
    // Gemini 호출 — 재시도(exponential backoff)
    // ──────────────────────────────────────────────────────────────────

    private String callGeminiForJsonArray(String userPrompt) {
        String systemInstruction =
                "당신은 프랜차이즈 본사(HQ) ERP 시스템의 구매 발주(Purchase Order) 전문 어시스턴트입니다.\n" +
                "분석 대상: 본사 중앙 창고(Central Warehouse)에 보관 중인 상품별 현재 재고·안전재고·최근 7일 소진량(매장 출고 기준).\n" +
                "목표: 외부 공급업체(Vendor)에 보낼 '구매 발주 초안(PO Draft)'을 생성한다.\n" +
                "리드타임 가정: 발주 후 입고까지 약 7일(1주)이 소요된다.\n\n" +
                "발주 판단 기준:\n" +
                "1. 현재재고 ≤ 안전재고(Safety Stock) → 즉시 발주 필요.\n" +
                "2. 현재재고가 (일평균 소진량 × 리드타임 7일) 이하 → 재고 부족 위험, 발주 권장.\n" +
                "3. 권장 발주 수량 = (안전재고 부족분) + (리드타임 7일치 소진량) — 최소 1 이상.\n\n" +
                "각 상품별로 반환: needsReorder(boolean), recommendQty(int, 불필요 시 0), " +
                "reason(String — 아래 포맷으로 한 문장 작성).\n" +
                "reason 포맷: '실재고(N개)가 안전재고(M개)에 미달하고, 주간 소진량(W개·리드타임 7일 감안) 대비 부족하여 X개 구매 발주를 제안합니다.'\n" +
                "발주 불필요 상품은 배열에서 생략 가능.\n" +
                "반드시 순수 JSON 배열만 반환하세요. 마크다운 코드블록(```json) 금지. 자연어 설명 금지.\n" +
                "형식: [{\"itemId\": 1, \"needsReorder\": true, \"recommendQty\": 50, \"reason\": \"실재고(5개)가 안전재고(20개)에 미달하고, 주간 소진량(21개·리드타임 7일 감안) 대비 부족하여 36개 구매 발주를 제안합니다.\"}]";

        Map<String, Object> requestBody = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                "contents", List.of(Map.of("parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig", Map.of("temperature", 0.1, "maxOutputTokens", 4096)
        );

        String url = GEMINI_BASE_URL + geminiModel + ":generateContent?key=" + apiKey;
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
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

            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                lastException = e;

                if ((status == 503 || status == 429) && attempt < MAX_RETRY_ATTEMPTS) {
                    long backoff = RETRY_BASE_BACKOFF_MS * attempt;
                    log.warn("[AI 자동 발주] Gemini API {} 응답 (attempt {}/{}). {}ms 후 재시도합니다.",
                            status, attempt, MAX_RETRY_ATTEMPTS, backoff);
                    sleep(backoff);
                } else {
                    log.error("[AI 자동 발주] Gemini API {} 에러 (최종 실패)", status, e);
                    throw new InoutException("Gemini API 서비스 오류(" + status + ")", 503, "AI_API_ERROR");
                }

            } catch (RestClientException e) {
                log.error("[AI 자동 발주] Gemini API 연결 실패 (attempt {})", attempt, e);
                lastException = e;
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    sleep(RETRY_BASE_BACKOFF_MS * attempt);
                } else {
                    throw new InoutException("Gemini API 서비스 연결에 실패했습니다.", 503, "AI_API_ERROR");
                }

            } catch (InoutException e) {
                throw e;
            } catch (Exception e) {
                log.error("[AI 자동 발주] Gemini 응답 파싱 실패", e);
                throw new InoutException("Gemini 응답 처리 중 오류가 발생했습니다.", 500, "AI_PARSE_ERROR");
            }
        }

        // 루프를 모두 소진한 경우 (이론상 도달하지 않지만 컴파일러를 위해)
        throw new InoutException("Gemini API 재시도 횟수 초과", 503, "AI_API_ERROR");
    }

    // ──────────────────────────────────────────────────────────────────
    // 규칙 기반 폴백 — AI 장애 시 안전재고/주간 판매량 기준 자동 산정
    // ──────────────────────────────────────────────────────────────────

    /**
     * AI 호출이 최종 실패한 경우 호출된다.
     * 규칙:
     *   1) 현재재고 ≤ 안전재고(minStockLevel) 인 상품 → 발주 필요
     *   2) 주간 판매량 > 0 이고 현재재고 < 주간 판매량 인 상품 → 발주 필요
     *   발주 수량 = 안전재고 부족분 + 주간 판매량(1주 버퍼), 최솟값 = 1
     */
    private List<AiOrderRecommendation> buildRuleBasedRecommendations(
            List<Item> items, Map<Long, Long> salesVelocityMap) {

        List<AiOrderRecommendation> result = new ArrayList<>();
        for (Item item : items) {
            int currentStock  = item.getCurrentStock();
            int safetyStock   = item.getMinStockLevel();
            long weeklyAvg    = salesVelocityMap.getOrDefault(item.getItemId(), 0L);

            boolean belowSafety = currentStock <= safetyStock;
            // 리드타임(7일) 동안 소진될 것으로 예상되는 수량보다 현재 재고가 적은 경우
            boolean underLeadTime = weeklyAvg > 0 && currentStock < weeklyAvg;

            if (!belowSafety && !underLeadTime) continue;

            // 권장 수량 = 안전재고 부족분 + 리드타임(7일)치 소진 버퍼
            int deficit      = Math.max(safetyStock - currentStock, 0);
            int leadBuffer   = (int) Math.min(weeklyAvg, Integer.MAX_VALUE); // 7일 소진량 ≒ LEAD_TIME_DAYS × 일평균
            int recommendQty = Math.max(deficit + leadBuffer, 1);

            String reason = belowSafety
                    ? String.format("실재고(%d개)가 안전재고(%d개)에 미달하고, 주간 소진량(%d개·리드타임 %d일 감안) 대비 부족하여 %d개 구매 발주를 제안합니다.",
                            currentStock, safetyStock, weeklyAvg, LEAD_TIME_DAYS, recommendQty)
                    : String.format("실재고(%d개)가 리드타임 %d일 예상 소진량(%d개)에 미달하여 %d개 구매 발주를 제안합니다.",
                            currentStock, LEAD_TIME_DAYS, weeklyAvg, recommendQty);

            result.add(new AiOrderRecommendation(item.getItemId(), true, recommendQty, reason));
        }
        return result;
    }

    // ──────────────────────────────────────────────────────────────────
    // 프롬프트 빌드
    // ──────────────────────────────────────────────────────────────────

    private String buildPrompt(List<Item> items, Map<Long, Long> salesVelocityMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("【본사 중앙 창고 재고 현황 — 구매 발주(PO) 분석 요청】\n");
        sb.append("기준: 최근 ").append(SALES_VELOCITY_DAYS).append("일간 매장 출고 소진량 / 리드타임 가정 7일\n\n");
        sb.append("상품 목록 (itemId | 품목명 | 현재재고 | 안전재고 | 최근")
          .append(SALES_VELOCITY_DAYS).append("일소진량 | 일평균소진 | 단가):\n");
        for (Item item : items) {
            long recentConsumption = salesVelocityMap.getOrDefault(item.getItemId(), 0L);
            double dailyAvg = recentConsumption / (double) SALES_VELOCITY_DAYS;
            sb.append(String.format(
                    "- itemId:%d | %s | 현재:%d | 안전:%d | %d일소진:%d | 일평균:%.1f | 단가:%,d원%n",
                    item.getItemId(),
                    item.getName(),
                    item.getCurrentStock(),
                    item.getMinStockLevel(),
                    SALES_VELOCITY_DAYS,
                    recentConsumption,
                    dailyAvg,
                    item.getUnitPrice()));
        }
        return sb.toString();
    }

    // ──────────────────────────────────────────────────────────────────
    // JSON 파싱
    // ──────────────────────────────────────────────────────────────────

    private List<AiOrderRecommendation> extractAndParseRecommendations(String rawText) {
        String cleaned = rawText
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        int start = cleaned.indexOf('[');
        int end   = cleaned.lastIndexOf(']');
        if (start == -1 || end == -1 || start > end) {
            log.error("[AI 자동 발주] JSON 배열 추출 실패. 원본: {}", rawText);
            throw new InoutException(
                    "AI 응답에서 JSON 배열을 찾을 수 없습니다. AI가 잘못된 형식을 반환했습니다.",
                    500, "AI_PARSE_ERROR");
        }
        String jsonArray = cleaned.substring(start, end + 1);

        try {
            return objectMapper.readValue(jsonArray, new TypeReference<List<AiOrderRecommendation>>() {});
        } catch (Exception e) {
            log.error("[AI 자동 발주] JSON 역직렬화 실패. jsonArray={}", jsonArray, e);
            throw new InoutException("AI 추천 데이터 파싱에 실패했습니다.", 500, "AI_DESERIALIZE_ERROR");
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // 유틸
    // ──────────────────────────────────────────────────────────────────

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("[AI 자동 발주] 재시도 대기 중 인터럽트 발생");
        }
    }
}
