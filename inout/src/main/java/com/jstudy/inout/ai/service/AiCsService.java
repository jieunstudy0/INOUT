package com.jstudy.inout.ai.service;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jstudy.inout.common.auth.entity.User;
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
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final int MAX_OUTPUT_TOKENS = 2000;
    private static final Set<String> VALID_CATEGORIES = Set.of("배송", "교환/환불", "기타");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 60_000;
    private static final String FALLBACK_DRAFT = "AI 답변 생성 중 오류가 발생했습니다. 직접 작성해 주세요.";

    /** 제어 문자(줄바꿈 등)가 포함된 AI 응답 JSON을 허용하는 전용 파서 */
    private static final ObjectMapper LENIENT_MAPPER = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .build();

    /** draftAnswer 값만 Greedy하게 추출하는 패턴 (JSON 파싱 실패 시 Fallback) */
    private static final Pattern DRAFT_ANSWER_PATTERN =
            Pattern.compile("\"draftAnswer\"\\s*:\\s*\"(.*?)\"(?:\\s*[,}])",
                    Pattern.DOTALL);

    private static final String TITLE_OWNER   = "점주님";
    private static final String TITLE_DEFAULT = "직원님";

    private record AiCsAnalysis(String category, String draftAnswer) {}

    /**
     * 작성자 역할(ROLE_OWNER / 그 외)에 따라 "{이름} {호칭}" 형식의 문자열을 반환합니다.
     * UserRole 컬렉션은 Lazy 로딩이므로 세션이 닫혀 있을 경우를 대비해 예외를 무시하고
     * 기본값("직원님")으로 폴백합니다.
     */
    private String resolveRequesterNameTitle(User author) {
        String name = "";
        try {
            if (author != null && StringUtils.hasText(author.getName())) {
                name = author.getName();
            }
        } catch (Exception ignored) {}

        String title = TITLE_DEFAULT;
        try {
            if (author != null) {
                boolean isOwner = author.getUserRoles().stream()
                        .map(ur -> ur.getRole() != null ? ur.getRole().getRoleName() : null)
                        .filter(Objects::nonNull)
                        .anyMatch("ROLE_OWNER"::equals);
                title = isOwner ? TITLE_OWNER : TITLE_DEFAULT;
            }
        } catch (Exception e) {
            log.debug("[AI CS 자동화] 역할 조회 실패 — 기본값('{}') 사용. cause={}", TITLE_DEFAULT, e.getMessage());
        }

        return StringUtils.hasText(name) ? name + " " + title : title;
    }

    /**
     * 작성자 호칭을 동적으로 바인딩한 시스템 프롬프트를 생성합니다.
     */
    private static String buildSystemInstruction(String requesterNameTitle) {
        return "당신은 프랜차이즈 본사(HQ)의 가맹점 지원 담당 AI입니다. " +
               "문의를 남긴 사람은 일반 소비자가 아니라 매장에서 근무하는 '점주' 또는 '직원'입니다.\n" +
               "제공된 문의 내용과 최근 주문/배송 내역을 참고하여 다음 두 가지를 수행하세요.\n" +
               "1) 문의 카테고리를 반드시 '배송', '교환/환불', '기타' 중 하나로만 분류하세요.\n" +
               "2) 본사 담당자가 검토 후 그대로 복사해서 답장할 수 있도록, " +
               "친절하고 정중한 한국어 답변 초안을 핵심만 3~4문장 이내로 간결하게 작성하세요. " +
               "필요하다면 최근 주문/배송 상태를 자연스럽게 반영하세요.\n\n" +
               "[필수 작성 규칙]\n" +
               "1. 시작 인사말은 반드시 '" + requesterNameTitle + "'을(를) 사용하여 시작하세요. " +
                  "(예: \"안녕하세요 " + requesterNameTitle + ", 문의해 주신...\")\n" +
               "2. '고객님'이라는 표현은 절대 사용하지 마세요.\n" +
               "3. 본사 담당자로서 매장 현장을 배려하는 공손하고 명확한 B2B 비즈니스 어조를 유지하세요.\n\n" +
               "[출력 규칙 — 반드시 준수]\n" +
               "- 오직 표준 JSON 형식만 출력하세요. 마크다운 코드블록(```json) · 서문 · 후문 텍스트를 절대 포함하지 마세요.\n" +
               "- JSON 내부 문자열에 줄바꿈이 필요한 경우 반드시 \\n으로 이스케이프 처리하세요.\n" +
               "- 응답 형식: {\"category\": \"배송\", \"draftAnswer\": \"안녕하세요 " + requesterNameTitle + ", ...\"}";
    }

    private final RestClient geminiRestClient = buildGeminiRestClient();

    private static RestClient buildGeminiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return RestClient.builder().requestFactory(factory).build();
    }

    // ─────────────────────────────────────────────────────────────
    // 단건 처리
    // ─────────────────────────────────────────────────────────────

    public void processSingleInquiry(Long inquiryId) {
        requireApiKey();

        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new InoutException("문의글을 찾을 수 없습니다.", 404, "INQUIRY_NOT_FOUND"));

        if (inquiry.getAiDraftAnswer() != null && !inquiry.getAiDraftAnswer().isBlank()) {
            throw new InoutException("이미 AI 초안이 생성된 문의입니다.", 409, "AI_DRAFT_ALREADY_EXISTS");
        }

        log.info("[AI CS 자동화] 단건 처리 시작 — inquiryId={}", inquiryId);

        AiCsAnalysis analysis = analyzeInquiry(inquiry);
        String category = resolveCategory(analysis.category());
        persistenceService.updateInquiryAnalysis(inquiryId, category, analysis.draftAnswer());

        evictDashboardCache();
        log.info("[AI CS 자동화] 단건 처리 완료 — inquiryId={}, category={}", inquiryId, category);
    }

    // ─────────────────────────────────────────────────────────────
    // 일괄 처리 (스케줄러)
    // ─────────────────────────────────────────────────────────────

    public int processWaitingInquiries() {
        requireApiKey();

        List<Inquiry> targets = inquiryRepository
                .findByAiDraftAnswerIsNullOrderByCreatedAtAsc(PageRequest.of(0, BATCH_SIZE));

        if (targets.isEmpty()) {
            log.info("[AI CS 자동화] 처리 대상 문의 없음. 건너뜀.");
            return 0;
        }
        log.info("[AI CS 자동화] 처리 대상 문의 {}건 감지. Gemini 분석 시작...", targets.size());

        int processedCount = 0;
        for (Inquiry inquiry : targets) {
            try {
                AiCsAnalysis analysis = analyzeInquiry(inquiry);
                String category = resolveCategory(analysis.category());
                if (persistenceService.updateInquiryAnalysis(inquiry.getId(), category, analysis.draftAnswer())) {
                    processedCount++;
                }
            } catch (Exception e) {
                log.warn("[AI CS 자동화] inquiryId={} 분석 실패. 건너뜀. 사유: {}", inquiry.getId(), e.getMessage());
            }
        }

        if (processedCount > 0) {
            evictDashboardCache();
        }
        log.info("[AI CS 자동화] 총 {}건 중 {}건 분석 완료", targets.size(), processedCount);
        return processedCount;
    }

    // ─────────────────────────────────────────────────────────────
    // 내부 핵심 로직
    // ─────────────────────────────────────────────────────────────

    private AiCsAnalysis analyzeInquiry(Inquiry inquiry) {
        List<OrderRequest> recentOrders = orderRequestRepository
                .findAllByRequestUser_IdOrderByRequestDateDesc(inquiry.getAuthor().getId())
                .stream()
                .limit(RECENT_ORDER_LIMIT)
                .toList();

        String requesterNameTitle = resolveRequesterNameTitle(inquiry.getAuthor());
        String systemInstruction  = buildSystemInstruction(requesterNameTitle);
        String userPrompt         = buildPrompt(inquiry, recentOrders);

        log.debug("[AI CS 자동화] 호칭 결정 — inquiryId={}, nameTitle='{}'",
                inquiry.getId(), requesterNameTitle);

        String rawText = callGeminiForJsonObject(userPrompt, systemInstruction);
        return extractAndParseAnalysis(rawText);
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

    private String callGeminiForJsonObject(String userPrompt, String systemInstruction) {
        Map<String, Object> requestBody = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                "contents", List.of(Map.of("parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "maxOutputTokens", MAX_OUTPUT_TOKENS
                )
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
            log.error("[AI CS 자동화] Gemini 응답 수신 실패", e);
            throw new InoutException("Gemini 응답 처리 중 오류가 발생했습니다.", 500, "AI_RESPONSE_ERROR");
        }
    }

    /**
     * AI 원본 텍스트에서 JSON 파싱 → 실패 시 Regex Fallback → 최종 Fallback 순으로 복구.
     * 절대 예외를 바깥으로 던지지 않습니다.
     */
    private AiCsAnalysis extractAndParseAnalysis(String rawText) {
        // 1단계: 마크다운 펜스 및 앞뒤 공백 제거
        String cleaned = rawText
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        // 2단계: { … } 경계 추출
        int start = cleaned.indexOf('{');
        int end   = cleaned.lastIndexOf('}');

        if (start == -1 || end == -1 || start >= end) {
            // JSON이 잘리거나 없는 경우 — Regex로 draftAnswer만 복구 시도
            log.warn("[AI CS 자동화] JSON 경계 탐지 실패 (토큰 초과 가능성). 원본 길이={}자. Fallback 적용.",
                    rawText.length());
            return new AiCsAnalysis("기타", recoverDraftAnswer(cleaned));
        }

        String jsonObject = cleaned.substring(start, end + 1);

        // 3단계: 제어 문자 허용 ObjectMapper로 파싱
        try {
            return LENIENT_MAPPER.readValue(jsonObject, AiCsAnalysis.class);
        } catch (Exception primary) {
            log.warn("[AI CS 자동화] 1차 파싱 실패 ({}). 제어문자 전처리 후 재시도.", primary.getMessage());
        }

        // 4단계: 제어 문자를 이스케이프 처리한 뒤 재시도
        try {
            String sanitized = escapeControlCharsInJsonStrings(jsonObject);
            return LENIENT_MAPPER.readValue(sanitized, AiCsAnalysis.class);
        } catch (Exception secondary) {
            log.warn("[AI CS 자동화] 2차 파싱도 실패. Regex Fallback 적용. 사유: {}", secondary.getMessage());
        }

        // 5단계: Regex로 값만 추출
        return new AiCsAnalysis("기타", recoverDraftAnswer(cleaned));
    }

    /**
     * JSON 문자열 값 내부의 리터럴 제어 문자(\n, \r, \t)를
     * JSON 이스케이프 표현으로 치환합니다.
     * JSON 구조 토큰(따옴표 밖) 영역의 공백은 그대로 둡니다.
     */
    private String escapeControlCharsInJsonStrings(String json) {
        StringBuilder sb = new StringBuilder(json.length());
        boolean inString = false;
        boolean escape   = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                sb.append(c);
                escape = false;
                continue;
            }
            if (c == '\\') {
                sb.append(c);
                escape = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                sb.append(c);
                continue;
            }
            if (inString) {
                switch (c) {
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default   -> sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * JSON 파싱 없이 Regex로 draftAnswer 값만 추출합니다.
     * JSON이 잘린 경우에도 부분 복구가 가능합니다.
     */
    private String recoverDraftAnswer(String text) {
        Matcher m = DRAFT_ANSWER_PATTERN.matcher(text);
        if (m.find()) {
            String value = m.group(1)
                    .replace("\\n", "\n")
                    .replace("\\r", "")
                    .replace("\\\"", "\"");
            log.info("[AI CS 자동화] Regex Fallback으로 draftAnswer 복구 성공.");
            return value;
        }

        // 마지막 수단: "draftAnswer": 이후 텍스트를 잘라내기
        int idx = text.indexOf("\"draftAnswer\"");
        if (idx != -1) {
            String after = text.substring(idx + "\"draftAnswer\"".length()).stripLeading();
            if (after.startsWith(":")) {
                after = after.substring(1).stripLeading();
            }
            if (after.startsWith("\"")) {
                after = after.substring(1);
                int quoteEnd = after.indexOf('"');
                if (quoteEnd > 0) {
                    return after.substring(0, quoteEnd);
                }
                // 끝 따옴표가 잘린 경우: 전체를 그대로 반환
                return after.isBlank() ? FALLBACK_DRAFT : after.strip();
            }
        }

        log.warn("[AI CS 자동화] draftAnswer 복구 불가. 기본 안내문으로 대체.");
        return FALLBACK_DRAFT;
    }

    // ─────────────────────────────────────────────────────────────
    // 유틸
    // ─────────────────────────────────────────────────────────────

    private String resolveCategory(String category) {
        return VALID_CATEGORIES.contains(category) ? category : "기타";
    }

    private void requireApiKey() {
        if (!StringUtils.hasText(apiKey)) {
            throw new InoutException(
                    "Gemini API 키가 설정되지 않았습니다. application-secret.properties에 gemini.api-key를 설정해 주세요.",
                    503, "GEMINI_NOT_CONFIGURED");
        }
    }

    private void evictDashboardCache() {
        try {
            dashboardService.evictDashboardSummary();
        } catch (RuntimeException ex) {
            log.warn("[AI CS 자동화] 대시보드 캐시 무효화 실패(무시). cause={}", ex.getMessage());
        }
    }
}
