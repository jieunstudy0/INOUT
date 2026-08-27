package com.jstudy.inout.delivery.service;

import com.jstudy.inout.delivery.dto.DeliveryTrackingDto;
import com.jstudy.inout.delivery.entity.Delivery;
import com.jstudy.inout.delivery.repository.DeliveryRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    private final DeliveryRepository deliveryRepository;
    private final RestClient restClient;
    private final String apiKey;
    private final String apiBaseUrl;

    public DeliveryTrackingService(
            DeliveryRepository deliveryRepository,
            @Value("${delivery.api.key:}") String apiKey,
            @Value("${delivery.api.base-url:https://info.sweettracker.co.kr/api/v1}") String apiBaseUrl) {
        this.deliveryRepository = deliveryRepository;
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
     * 승인/배송 시작 시각 기준으로 3일(72시간) 경과에 따라
     * 집화처리 → 간선수송 → 배송출발 → 배송완료 단계로 동적으로 노출한다.
     */
    private DeliveryTrackingDto.TrackingResponse buildMockTimeline(
            String carrier, String trackingNumber, String note) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startAt = resolveMockStartAt(trackingNumber, now);
        long elapsedHours = Math.max(0L, Duration.between(startAt, now).toHours());

        int stage;
        String currentStatus;
        if (elapsedHours < 24) {
            stage = 1;
            currentStatus = "집화처리";
        } else if (elapsedHours < 48) {
            stage = 2;
            currentStatus = "간선수송";
        } else if (elapsedHours < 72) {
            stage = 3;
            currentStatus = "배송출발";
        } else {
            stage = 4;
            currentStatus = "배송완료";
        }

        List<DeliveryTrackingDto.TrackingEvent> events = new ArrayList<>();
        events.add(DeliveryTrackingDto.TrackingEvent.builder()
                .time(startAt)
                .location("서울특별시 송파구 집화센터")
                .status("집화처리")
                .description("상품이 집화센터에 등록되었습니다." + (note != null ? " (" + note + ")" : ""))
                .build());
        if (stage >= 2) {
            events.add(DeliveryTrackingDto.TrackingEvent.builder()
                    .time(startAt.plusHours(24))
                    .location("옥천HUB")
                    .status("간선수송")
                    .description("옥천HUB로 이동 후 간선하차 처리되었습니다.")
                    .build());
        }
        if (stage >= 3) {
            events.add(DeliveryTrackingDto.TrackingEvent.builder()
                    .time(startAt.plusHours(48))
                    .location("배송지 관할 지점")
                    .status("배송출발")
                    .description("배송지 관할 지점에서 입고/배송 출발 처리되었습니다.")
                    .build());
        }
        if (stage >= 4) {
            events.add(DeliveryTrackingDto.TrackingEvent.builder()
                    .time(startAt.plusHours(72))
                    .location("본사 중앙창고")
                    .status("배송완료")
                    .description("가상 공급처 발주 건이 창고에 입고 완료되었습니다.")
                    .build());
        }

        return DeliveryTrackingDto.TrackingResponse.builder()
                .carrier(carrier)
                .trackingNumber(trackingNumber)
                .currentStatus(currentStatus)
                .mockFallback(true)
                .events(events)
                .build();
    }

    private LocalDateTime resolveMockStartAt(String trackingNumber, LocalDateTime fallbackNow) {
        if (!StringUtils.hasText(trackingNumber)) {
            return fallbackNow;
        }
        List<Delivery> matched = deliveryRepository.findAllByTrackingNumberWithOrder(trackingNumber.trim());
        if (matched == null || matched.isEmpty()) {
            return fallbackNow;
        }
        // 혹시 과거 데이터에 중복 송장이 있어도 최신 건 기준으로 타임라인을 계산한다.
        return resolveStartFromDelivery(matched.get(0));
    }

    private LocalDateTime resolveStartFromDelivery(Delivery delivery) {
        LocalDateTime approvedAt = delivery.getOrderRequest() != null
                ? delivery.getOrderRequest().getProcessDate()
                : null;
        if (approvedAt != null) {
            return approvedAt;
        }
        if (delivery.getShippedAt() != null) {
            return delivery.getShippedAt();
        }
        if (delivery.getCreatedAt() != null) {
            return delivery.getCreatedAt();
        }
        return LocalDateTime.now();
    }
}
