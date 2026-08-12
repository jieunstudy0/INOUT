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
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
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

    @Value("${gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int SALES_VELOCITY_DAYS = 7;
    private static final int MAX_ITEMS_PER_ANALYSIS = 300;
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    private final RestClient geminiRestClient = buildGeminiRestClient();

    private static RestClient buildGeminiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return RestClient.builder().requestFactory(factory).build();
    }

    record AiOrderRecommendation(Long itemId, boolean needsReorder, int recommendQty, String reason) {}

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

        String userPrompt = buildPrompt(activeItems, salesVelocityMap);
        String rawResponse = callGeminiForJsonArray(userPrompt);

        List<AiOrderRecommendation> recommendations = extractAndParseRecommendations(rawResponse).stream()
                .filter(AiOrderRecommendation::needsReorder)
                .toList();
        if (recommendations.isEmpty()) {
            log.info("[AI 자동 발주] Gemini 분석 결과 재고 보충이 시급한 상품 없음.");
            return 0;
        }
        log.info("[AI 자동 발주] Gemini 추천 결과 {}건 수신. 통합 발주 초안 저장 시작...", recommendations.size());

        Map<Long, Item> itemMap = activeItems.stream()
                .collect(Collectors.toMap(Item::getItemId, item -> item));
        return persistenceService.saveDraftOrder(itemMap, recommendations);
    }

    private String buildPrompt(List<Item> items, Map<Long, Long> salesVelocityMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("아래는 전체 활성 상품의 재고 및 최근 ").append(SALES_VELOCITY_DAYS).append("일간 판매 데이터입니다.\n");
        sb.append("각 상품의 현재 재고와 판매 추이를 분석해 재고 보충이 시급한 상품을 선별하고 발주 수량을 추천해 주세요.\n\n");
        sb.append("상품 목록:\n");
        for (Item item : items) {
            long recentSales = salesVelocityMap.getOrDefault(item.getItemId(), 0L);
            sb.append(String.format(
                    "- itemId: %d, 품목명: %s, 현재재고: %d, 안전재고: %d, 최근%d일판매량: %d, 단가: %,d원%n",
                    item.getItemId(),
                    item.getName(),
                    item.getCurrentStock(),
                    item.getMinStockLevel(),
                    SALES_VELOCITY_DAYS,
                    recentSales,
                    item.getUnitPrice()));
        }
        return sb.toString();
    }

    private String callGeminiForJsonArray(String userPrompt) {
        String systemInstruction =
                "당신은 재고 관리 시스템의 지능형 자동 발주 어시스턴트입니다. " +
                "각 상품의 현재 재고, 안전 재고 기준, 최근 판매 추이(Sales Velocity)를 종합적으로 분석해서 " +
                "재고 보충이 시급한 상품을 골라내세요. " +
                "각 상품별로 1) 발주 필요 여부(needsReorder: boolean), 2) 적정 발주 권장 수량(recommendQty: int, " +
                "필요 없으면 0), 3) 추천 사유(reason: String, 예: '최근 일주일 판매량이 급증하여 3일 내 품절 예상됨')를 산출하세요. " +
                "발주가 필요 없다고 판단한 상품은 배열에서 제외해도 됩니다. " +
                "반드시 다른 자연어 설명 없이 아래 형식의 순수 JSON 배열만 반환하세요. " +
                "마크다운 코드블록(```json)을 사용하지 마세요. 오직 JSON 텍스트만 출력하세요.\n" +
                "형식: [{\"itemId\": 1, \"needsReorder\": true, \"recommendQty\": 50, \"reason\": \"안전재고 미달 및 판매량 급증\"}]";

        Map<String, Object> requestBody = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                "contents", List.of(Map.of("parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig", Map.of("temperature", 0.1, "maxOutputTokens", 4096)
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
            log.error("[AI 자동 발주] Gemini API 호출 실패", e);
            throw new InoutException("Gemini API 서비스 연결에 실패했습니다.", 503, "AI_API_ERROR");
        } catch (InoutException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AI 자동 발주] Gemini 응답 파싱 실패", e);
            throw new InoutException("Gemini 응답 처리 중 오류가 발생했습니다.", 500, "AI_PARSE_ERROR");
        }
    }

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
}
