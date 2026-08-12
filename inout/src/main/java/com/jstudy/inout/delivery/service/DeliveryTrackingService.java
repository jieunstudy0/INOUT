package com.jstudy.inout.delivery.service;

import com.jstudy.inout.delivery.dto.DeliveryTrackingDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 서드파티 배송조회 프록시.
 * API 키/응답이 없거나 Mock 송장(56xxxxxxxxxx)인 경우 포트폴리오 시연용 Dummy 타임라인을 반환한다.
 */
@Slf4j
@Service
public class DeliveryTrackingService {

    private final RestClient restClient;
    private final String apiKey;
    private final String apiBaseUrl;

    public DeliveryTrackingService(
            @Value("${delivery.api.key:}") String apiKey,
            @Value("${delivery.api.base-url:https://info.sweettracker.co.kr/api/v1}") String apiBaseUrl) {
        this.apiKey = apiKey;
        this.apiBaseUrl = apiBaseUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public DeliveryTrackingDto.TrackingResponse track(String carrier, String trackingNumber) {
        String safeCarrier = StringUtils.hasText(carrier) ? carrier.trim() : DeliveryService.CARRIER_CJ;
        String safeNumber = trackingNumber != null ? trackingNumber.trim() : "";

        if (!StringUtils.hasText(safeNumber)) {
            return buildMockTimeline(safeCarrier, safeNumber, "운송장 번호 없음");
        }

        // Mock 발급 번호(56으로 시작하는 12자리)는 외부에 존재하지 않으므로 즉시 Fallback
        if (isMockCjTrackingNumber(safeNumber) || !StringUtils.hasText(apiKey)) {
            log.info("[배송조회] Mock/키미설정 Fallback — carrier={}, trackingNumber={}", safeCarrier, safeNumber);
            return buildMockTimeline(safeCarrier, safeNumber, null);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restClient.get()
                    .uri(apiBaseUrl + "/trackingInfo?t_key={key}&t_code={code}&t_invoice={invoice}",
                            apiKey, resolveCarrierCode(safeCarrier), safeNumber)
                    .retrieve()
                    .body(Map.class);

            if (body == null || body.isEmpty() || isExternalEmpty(body)) {
                log.info("[배송조회] 외부 응답 없음 → Fallback — trackingNumber={}", safeNumber);
                return buildMockTimeline(safeCarrier, safeNumber, null);
            }

            return mapExternalResponse(safeCarrier, safeNumber, body);
        } catch (RestClientException ex) {
            log.warn("[배송조회] 외부 API 실패 → Fallback: {}", ex.getMessage());
            return buildMockTimeline(safeCarrier, safeNumber, null);
        } catch (Exception ex) {
            log.warn("[배송조회] 예외 → Fallback: {}", ex.getMessage());
            return buildMockTimeline(safeCarrier, safeNumber, null);
        }
    }

    private boolean isMockCjTrackingNumber(String trackingNumber) {
        return trackingNumber != null && trackingNumber.matches("^56\\d{10}$");
    }

    private boolean isExternalEmpty(Map<String, Object> body) {
        Object trackingDetails = body.get("trackingDetails");
        if (trackingDetails instanceof List<?> list) {
            return list.isEmpty();
        }
        Object status = body.get("status");
        return status == null && !body.containsKey("level");
    }

    @SuppressWarnings("unchecked")
    private DeliveryTrackingDto.TrackingResponse mapExternalResponse(
            String carrier, String trackingNumber, Map<String, Object> body) {
        Object detailsObj = body.get("trackingDetails");
        if (!(detailsObj instanceof List<?> details) || details.isEmpty()) {
            return buildMockTimeline(carrier, trackingNumber, null);
        }

        List<DeliveryTrackingDto.TrackingEvent> events = details.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .map(item -> DeliveryTrackingDto.TrackingEvent.builder()
                        .time(LocalDateTime.now())
                        .location(String.valueOf(item.getOrDefault("where", "-")))
                        .status(String.valueOf(item.getOrDefault("kind", "이동")))
                        .description(String.valueOf(item.getOrDefault("remark", "")))
                        .build())
                .toList();

        if (events.isEmpty()) {
            return buildMockTimeline(carrier, trackingNumber, null);
        }

        return DeliveryTrackingDto.TrackingResponse.builder()
                .carrier(carrier)
                .trackingNumber(trackingNumber)
                .currentStatus(events.get(events.size() - 1).getStatus())
                .mockFallback(false)
                .events(events)
                .build();
    }

    private String resolveCarrierCode(String carrier) {
        if (carrier.contains("한진")) return "05";
        if (carrier.contains("롯데")) return "08";
        if (carrier.contains("우체국")) return "01";
        return "04"; // CJ대한통운 (SweetTracker 코드)
    }

    /**
     * 집화처리 → 옥천HUB 간선하차 → 배송출발 → 배송완료
     */
    private DeliveryTrackingDto.TrackingResponse buildMockTimeline(
            String carrier, String trackingNumber, String note) {
        LocalDateTime now = LocalDateTime.now();
        List<DeliveryTrackingDto.TrackingEvent> events = List.of(
                DeliveryTrackingDto.TrackingEvent.builder()
                        .time(now.minusHours(30))
                        .location("서울특별시 송파구 집화센터")
                        .status("집화처리")
                        .description("상품이 집화되었습니다." + (note != null ? " (" + note + ")" : ""))
                        .build(),
                DeliveryTrackingDto.TrackingEvent.builder()
                        .time(now.minusHours(18))
                        .location("옥천HUB")
                        .status("옥천HUB 간선하차")
                        .description("옥천HUB에 도착하여 간선하차 처리되었습니다.")
                        .build(),
                DeliveryTrackingDto.TrackingEvent.builder()
                        .time(now.minusHours(6))
                        .location("경기광주 배송점")
                        .status("배송출발")
                        .description("배송 기사가 배송을 시작했습니다.")
                        .build(),
                DeliveryTrackingDto.TrackingEvent.builder()
                        .time(now.minusHours(1))
                        .location("수령지 인근")
                        .status("배송완료")
                        .description("고객님께 배송이 완료되었습니다.")
                        .build()
        );

        return DeliveryTrackingDto.TrackingResponse.builder()
                .carrier(carrier)
                .trackingNumber(trackingNumber)
                .currentStatus("배송완료")
                .mockFallback(true)
                .events(events)
                .build();
    }
}
