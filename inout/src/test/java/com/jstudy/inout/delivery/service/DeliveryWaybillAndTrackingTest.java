package com.jstudy.inout.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.jstudy.inout.delivery.dto.DeliveryDto;
import com.jstudy.inout.delivery.dto.DeliveryTrackingDto;
import com.jstudy.inout.delivery.entity.Delivery;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import com.jstudy.inout.delivery.repository.DeliveryRepository;
import com.jstudy.inout.order.entity.OrderRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryWaybillAndTrackingTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @InjectMocks
    private DeliveryService deliveryService;

    @Test
    @DisplayName("운송장 Mock 발급 — CJ 형식(56 + 10자리) 저장")
    void generateWaybill_savesCjStyleNumber() {
        OrderRequest order = OrderRequest.builder().id(10L).build();
        Delivery delivery = Delivery.builder()
                .orderRequest(order)
                .status(DeliveryStatus.READY)
                .receiverName("홍길동")
                .receiverPhone("010")
                .destinationAddress("서울")
                .build();
        // id reflection not needed if we stub findByIdForUpdate returning this instance
        given(deliveryRepository.findByIdForUpdate(1L)).willReturn(Optional.of(delivery));

        DeliveryDto.DetailResponse result = deliveryService.generateWaybill(1L);

        assertThat(result.getTrackingNumber()).matches("^56\\d{10}$");
        assertThat(result.getCarrier()).isEqualTo("CJ대한통운");
        assertThat(delivery.getTrackingNumber()).isEqualTo(result.getTrackingNumber());
    }

    @Test
    @DisplayName("배송조회 — Mock 송장/키 미설정 시 Fallback 타임라인")
    void track_returnsMockTimelineForCjMockNumber() {
        DeliveryTrackingService service = new DeliveryTrackingService("", "https://example.invalid");

        DeliveryTrackingDto.TrackingResponse res = service.track("CJ대한통운", "561234567890");

        assertThat(res.isMockFallback()).isTrue();
        assertThat(res.getEvents()).hasSize(4);
        assertThat(res.getEvents().get(0).getStatus()).isEqualTo("집화처리");
        assertThat(res.getEvents().get(1).getStatus()).contains("간선");
        assertThat(res.getEvents().get(2).getStatus()).isEqualTo("배송출발");
        assertThat(res.getEvents().get(3).getStatus()).isEqualTo("배송완료");
    }

    @Test
    @DisplayName("기존 구형 송장(CJ…) 백필 — CJ대한통운 + 56xxxxxxxxxx")
    void backfillMockWaybills_updatesLegacyCompleted() {
        OrderRequest order = OrderRequest.builder().id(20L).build();
        Delivery legacy = Delivery.builder()
                .orderRequest(order)
                .status(DeliveryStatus.COMPLETED)
                .receiverName("홍길동")
                .receiverPhone("010")
                .destinationAddress("서울")
                .trackingNumber("CJ1730000000000")
                .build();
        given(deliveryRepository.findAll()).willReturn(List.of(legacy));

        int updated = deliveryService.backfillMockWaybills();

        assertThat(updated).isEqualTo(1);
        assertThat(legacy.getCarrier()).isEqualTo("CJ대한통운");
        assertThat(legacy.getTrackingNumber()).matches("^56\\d{10}$");
    }
}
