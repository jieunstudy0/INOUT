package com.jstudy.inout.ai.service;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.stock.dto.count.StockCountLineResponse;
import com.jstudy.inout.stock.entity.StockCountSheet.AiReading;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 재고 실사 영수증 사진에서 손글씨 실재고를 읽는다 (Gemini 이미지 입력).
 *
 * 읽은 값은 참고용이다. 화면에서 직원이 확인·수정해 제출하고, 본사가 승인해야 재고에 반영된다.
 * 그래서 확실하지 않은 숫자는 추측하지 말고 비워 두라고(null) 지시하고, 응답도 그 기준으로 다시 검사한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiStockCountReader {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    /** 이미지 분석은 글자만 보낼 때보다 오래 걸려 읽기 시간 제한을 넉넉히 둔다. */
    private static final int READ_TIMEOUT_MS = 60_000;
    private static final int MAX_RETRY_ATTEMPTS = 2;
    private static final long RETRY_BACKOFF_MS = 1_500L;
    private static final int MAX_QTY = 99_999;

    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    private final RestClient restClient = buildRestClient();

    private static RestClient buildRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return RestClient.builder().requestFactory(factory).build();
    }

    /** 사진에서 읽은 결과. sheetCode는 영수증 상단 번호(못 읽으면 null). */
    public record Result(String sheetCode, Map<Integer, AiReading> readings) {
    }

    public Result read(byte[] image, String mimeType, String expectedSheetCode, List<StockCountLineResponse> lines) {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("[재고 실사 AI] gemini.api-key 미설정 — application-secret.properties를 확인하세요.");
            throw new InoutException(ErrorCode.AI_NOT_CONFIGURED,
                    "AI 읽기를 사용할 수 없습니다. 영수증을 보고 실재고를 직접 입력해 주세요.");
        }
        String text = callGemini(image, mimeType, buildPrompt(expectedSheetCode, lines));
        Set<Integer> validLineNos = lines.stream().map(StockCountLineResponse::getLineNo).collect(Collectors.toSet());
        return parse(text, validLineNos);
    }

    String buildPrompt(String expectedSheetCode, List<StockCountLineResponse> lines) {
        // 시스템 수량은 일부러 프롬프트에 넣지 않는다. 넣으면 손글씨 대신 그 값을 그대로 답할 위험이 커진다.
        String lineList = lines.stream()
                .map(l -> String.format("%d. %s (위치 %s)", l.getLineNo(), l.getItemName(),
                        l.getLocation() == null ? "미지정" : l.getLocation()))
                .collect(Collectors.joining("\n"));
        return """
                이 사진은 창고 재고 실사 영수증이다. 영수증 맨 위에 실사표 번호(예: %s)가 인쇄되어 있다.
                각 줄은 [번호 | 상품명 | 위치 | 기록(인쇄된 수량) | 실재고 칸] 순서이고, 실재고 칸에는 직원이 펜으로 숫자를 적었다.

                할 일: 줄 번호별로 '실재고' 칸의 손글씨 숫자만 읽어라.
                규칙:
                - 인쇄된 숫자(기록 칸)를 실재고로 옮기지 마라. 손으로 쓴 숫자만 읽는다.
                - 칸이 비었거나, 줄을 그어 지웠거나, 숫자를 알아볼 수 없으면 countedQty는 null, confident는 false.
                - 숫자는 읽었지만 다른 숫자와 헷갈릴 수 있으면(예: 1과 7, 4와 9, 0과 6) confident는 false.
                - 고쳐 쓴 숫자가 있으면 마지막에 쓴 숫자를 읽고 confident는 false.
                - 아래 목록에 없는 줄 번호는 만들지 마라.

                줄 목록:
                %s

                JSON 객체 하나만 반환하라:
                {"sheetCode": "영수증 상단 번호 또는 null", "lines": [{"lineNo": 1, "countedQty": 3, "confident": true}]}
                """.formatted(expectedSheetCode, lineList);
    }

    private String callGemini(byte[] image, String mimeType, String prompt) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(
                        Map.of("inline_data", Map.of(
                                "mime_type", mimeType,
                                "data", Base64.getEncoder().encodeToString(image))),
                        Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", 0,
                        "responseMimeType", "application/json"));

        // API 키는 URL이 아니라 헤더로 보낸다 — URL은 프록시·오류 로그에 그대로 남을 수 있다.
        String url = GEMINI_BASE_URL + geminiModel + ":generateContent";

        for (int attempt = 1; ; attempt++) {
            try {
                String response = restClient.post()
                        .uri(url)
                        .header("Content-Type", "application/json")
                        .header("x-goog-api-key", apiKey)
                        .body(body)
                        .retrieve()
                        .body(String.class);
                JsonNode candidates = objectMapper.readTree(response).path("candidates");
                if (!candidates.isArray() || candidates.isEmpty()) {
                    log.error("[재고 실사 AI] 응답에 candidates 없음");
                    throw new InoutException(ErrorCode.AI_INVALID_RESPONSE);
                }
                return candidates.get(0).path("content").path("parts").path(0).path("text").asText();

            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                if ((status == 429 || status == 503) && attempt < MAX_RETRY_ATTEMPTS) {
                    log.warn("[재고 실사 AI] Gemini {} 응답 (attempt {}/{}), 재시도", status, attempt, MAX_RETRY_ATTEMPTS);
                    sleep(RETRY_BACKOFF_MS * attempt);
                    continue;
                }
                log.error("[재고 실사 AI] Gemini {} 에러 (최종 실패)", status);
                throw new InoutException(ErrorCode.AI_API_ERROR,
                        "AI가 사진을 읽지 못했습니다. 잠시 후 다시 올리거나 실재고를 직접 입력해 주세요.");
            } catch (ResourceAccessException e) {
                log.error("[재고 실사 AI] Gemini 연결 실패·시간 초과", e);
                throw new InoutException(ErrorCode.AI_TIMEOUT,
                        "AI 응답이 늦어 사진을 읽지 못했습니다. 다시 올리거나 실재고를 직접 입력해 주세요.");
            } catch (RestClientException e) {
                log.error("[재고 실사 AI] Gemini 호출 실패", e);
                throw new InoutException(ErrorCode.AI_API_ERROR);
            } catch (InoutException e) {
                throw e;
            } catch (Exception e) {
                // 외부 API 응답 처리 경계: 응답 형식이 달라도 500 대신 정해진 AI 오류로 돌려준다.
                log.error("[재고 실사 AI] 응답 처리 실패", e);
                throw new InoutException(ErrorCode.AI_PARSE_ERROR);
            }
        }
    }

    /**
     * 모델이 준 JSON을 검사해 받아들일 값만 남긴다.
     * 목록에 없는 줄 번호, 정수가 아니거나 범위를 벗어난 수량은 "읽지 못함"(null)으로 바꾼다.
     */
    Result parse(String text, Set<Integer> validLineNos) {
        JsonNode root;
        try {
            root = objectMapper.readTree(stripCodeFence(text));
        } catch (Exception e) {
            log.error("[재고 실사 AI] JSON 파싱 실패");
            throw new InoutException(ErrorCode.AI_PARSE_ERROR);
        }

        JsonNode codeNode = root.path("sheetCode");
        String sheetCode = codeNode.isTextual() && !codeNode.asText().isBlank()
                && !"null".equalsIgnoreCase(codeNode.asText()) ? codeNode.asText().trim() : null;

        Map<Integer, AiReading> readings = new HashMap<>();
        for (JsonNode node : root.path("lines")) {
            if (!node.path("lineNo").canConvertToInt()) {
                continue;
            }
            int lineNo = node.path("lineNo").asInt();
            if (!validLineNos.contains(lineNo)) {
                continue;
            }
            JsonNode qtyNode = node.path("countedQty");
            Integer qty = null;
            if (qtyNode.isIntegralNumber() && qtyNode.asInt() >= 0 && qtyNode.asInt() <= MAX_QTY) {
                qty = qtyNode.asInt();
            }
            boolean confident = qty != null && node.path("confident").asBoolean(false);
            readings.put(lineNo, new AiReading(qty, confident));
        }
        return new Result(sheetCode, readings);
    }

    private static String stripCodeFence(String text) {
        String t = text == null ? "" : text.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        return t;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InoutException(ErrorCode.AI_INTERRUPTED);
        }
    }
}
