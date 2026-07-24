package com.jstudy.inout.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jstudy.inout.ai.dto.AiInsightResponse;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiInsightService {

    private final ItemRepository itemRepository;
    private final OrderRequestRepository orderRequestRepository;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String apiKey;

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    @Async("applicationTaskExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<AiInsightResponse> generateInsightReport() {
        if (!StringUtils.hasText(apiKey)) {
            throw new InoutException(
                    "Gemini API 키가 설정되지 않았습니다. application-secret.properties에 gemini.api-key를 설정해 주세요.",
                    503, "AI_NOT_CONFIGURED");
        }

        try {
            String prompt = buildPrompt(); 
            log.info("Gemini 인사이트 프롬프트 생성 완료");

            String reportText = callGemini(prompt);
            String generatedAt = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm"));

            return CompletableFuture.completedFuture(
                    AiInsightResponse.builder()
                            .report(reportText)
                            .generatedAt(generatedAt)
                            .model("gemini-2.5-flash")
                            .build());

        } catch (InoutException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 인사이트 생성 중 오류", e);
            throw new InoutException("AI 분석 중 오류가 발생했습니다.", 500, "AI_SERVICE_ERROR");
        }
    }

    private String buildPrompt() {
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        LocalDateTime oneMonthAgo  = LocalDateTime.now().minusMonths(1);

        List<Object[]> monthlyData    = orderRequestRepository.findMonthlyOrderAmountTrend(sixMonthsAgo);
        List<Object[]> topStores      = orderRequestRepository.findTopStoreOrderFrequency(oneMonthAgo, PageRequest.of(0, 5));
        long lowStockCount            = itemRepository.countLowStockItems();
        long outOfStockCount          = itemRepository.countOutOfStockItems();
        long totalActiveItems         = itemRepository.countActiveItems();
        long completedCount           = orderRequestRepository.countByStatus(OrderStatus.COMPLETED);
        long pendingCount             = orderRequestRepository.countByStatus(OrderStatus.PAID);
        long rejectedCount            = orderRequestRepository.countByStatus(OrderStatus.REJECTED);
        List<Item> outOfStockItems    = itemRepository.findOutOfStockItems();
        List<Item> lowStockItems      = itemRepository.findLowStockItems()
                .stream().filter(i -> i.getCurrentStock() > 0).limit(5).toList();

        StringBuilder sb = new StringBuilder();
        sb.append("## INOUT B2B 발주/재고 관리 시스템 운영 데이터\n\n");

        sb.append("### 1. 최근 6개월 월별 발주 금액\n");
        if (monthlyData.isEmpty()) {
            sb.append("- 데이터 없음\n");
        } else {
            for (Object[] row : monthlyData) {
                sb.append(String.format("- %d년 %02d월: %,d원%n",
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).longValue()));
            }
        }

        sb.append("\n### 2. 현재 재고 현황\n");
        sb.append(String.format("- 전체 관리 상품 수: %d개%n", totalActiveItems));
        sb.append(String.format("- 저재고(안전재고 미만) 상품 수: %d개%n", lowStockCount));
        sb.append(String.format("- 품절 상품 수: %d개%n", outOfStockCount));

        if (!outOfStockItems.isEmpty()) {
            String names = outOfStockItems.stream().limit(5)
                    .map(Item::getName).collect(Collectors.joining(", "));
            sb.append("- 품절 상품: ").append(names).append("\n");
        }
        if (!lowStockItems.isEmpty()) {
            sb.append("- 저재고 상품 현황: ");
            lowStockItems.forEach(i -> sb.append(
                    String.format("%s(현재 %d개/최소 %d개) ", i.getName(), i.getCurrentStock(), i.getMinStockLevel())));
            sb.append("\n");
        }

        sb.append("\n### 3. 누적 발주 처리 현황\n");
        sb.append(String.format("- 승인 완료: %d건%n", completedCount));
        sb.append(String.format("- 결제 완료(승인 대기): %d건%n", pendingCount));
        sb.append(String.format("- 반려 처리: %d건%n", rejectedCount));

        sb.append("\n### 4. 최근 1개월 매장별 발주 빈도 Top 5\n");
        if (topStores.isEmpty()) {
            sb.append("- 데이터 없음\n");
        } else {
            for (Object[] row : topStores) {
                sb.append(String.format("- %s: %d건%n", row[0], ((Number) row[1]).longValue()));
            }
        }

        return sb.toString();
    }

    private String callGemini(String userPrompt) {
        String systemInstruction = "당신은 B2B 유통/발주 시스템의 운영 분석 전문가입니다. 제공된 시스템 운영 데이터를 분석하여 실무적인 인사이트를 한국어로 작성하세요. 반드시 다음 형식을 지키세요:\n📊 [인사이트 제목]\n내용...\n\n💡 종합 권고사항\n내용...";
        
   
        Map<String, Object> requestBody = Map.of(
            "system_instruction", Map.of("parts", List.of(Map.of("text", systemInstruction))), // ✅ 올바른 규격 (배열 형태)
            "contents", List.of(Map.of("parts", List.of(Map.of("text", userPrompt)))),
            "generationConfig", Map.of("temperature", 0.5, "maxOutputTokens", 4096)
        );

        RestClient restClient = RestClient.create();
        try {
            String responseBody = restClient.post()

                    .uri(GEMINI_URL + "?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");

            if (candidates.isEmpty()) {
                throw new InoutException("AI 응답 형식이 올바르지 않습니다.", 500, "AI_INVALID_RESPONSE");
            }

            return candidates.get(0).path("content").path("parts").get(0).path("text").asText();

        } catch (RestClientException e) {
            log.error("Gemini API 호출 실패", e);
            throw new InoutException("AI 서비스 연결에 실패했습니다.", 503, "AI_API_ERROR");
        } catch (Exception e) {
            log.error("Gemini 응답 파싱 실패", e);
            throw new InoutException("AI 응답 처리 중 오류가 발생했습니다.", 500, "AI_PARSE_ERROR");
        }
    }
}
