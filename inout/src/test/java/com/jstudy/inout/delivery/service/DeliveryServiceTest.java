package com.jstudy.inout.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.config.JpaAuditConfig;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.delivery.dto.DeliveryDto;
import com.jstudy.inout.delivery.entity.Delivery;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import com.jstudy.inout.delivery.repository.DeliveryRepository;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.order.testsupport.OrderJpaTestApplication;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ActiveProfiles("jpa-slice")
@ContextConfiguration(classes = OrderJpaTestApplication.class)
@Import({JpaAuditConfig.class, DeliveryService.class})
class DeliveryServiceTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private OrderRequestRepository orderRequestRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private DeliveryService deliveryService;

    private Store store;
    private User requester;

    @BeforeEach
    void setUp() {
        store = Store.builder()
                .name("테스트매장")
                .address("서울시 강남구 테헤란로 1")
                .build();
        em.persist(store);

        requester = User.builder()
                .email("req@test.com")
                .password("pw")
                .name("신청자")
                .phone("010-0000-0001")
                .birthday(LocalDate.of(1990, 1, 1))
                .store(store)
                .deleted(false)
                .build();
        em.persist(requester);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("COMPLETED 발주에 대해 배송 행이 없으면 OrderRequest에 확정된 배송 스냅샷으로 Delivery를 생성한다")
    void createDeliveryIfAbsentForCompletedOrder_createsFromOrder() {
        OrderRequest order = orderRequestRepository.save(OrderRequest.builder()
                .requestUser(em.find(User.class, requester.getId()))
                .status(OrderStatus.APPROVED)
                .totalPrice(1000L)
                .requestDate(LocalDateTime.now())
                .receiverName("확정수령인")
                .receiverPhone("010-7777-7777")
                .destinationAddress("인천 송도 물류센터")
                .build());

        deliveryService.createDeliveryIfAbsentForCompletedOrder(
                orderRequestRepository.findById(order.getId()).orElseThrow());

        Delivery saved = deliveryRepository.findByOrderRequest_Id(order.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(DeliveryStatus.READY);
        assertThat(saved.getReceiverName()).isEqualTo("확정수령인");
        assertThat(saved.getReceiverPhone()).isEqualTo("010-7777-7777");
        assertThat(saved.getDestinationAddress()).isEqualTo("인천 송도 물류센터");
    }

    @Test
    @DisplayName("배송 생성은 동일 주문에 대해 멱등하다")
    void createDeliveryIfAbsentForCompletedOrder_idempotent() {
        OrderRequest order = orderRequestRepository.save(OrderRequest.builder()
                .requestUser(em.find(User.class, requester.getId()))
                .status(OrderStatus.APPROVED)
                .totalPrice(1000L)
                .requestDate(LocalDateTime.now())
                .receiverName("신청자")
                .receiverPhone("010-0000-0001")
                .destinationAddress("서울시 강남구 테헤란로 1")
                .build());

        OrderRequest managed = orderRequestRepository.findById(order.getId()).orElseThrow();
        deliveryService.createDeliveryIfAbsentForCompletedOrder(managed);
        deliveryService.createDeliveryIfAbsentForCompletedOrder(managed);

        assertThat(deliveryRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("COMPLETED가 아니면 배송 행을 만들지 않는다")
    void createDeliveryIfAbsentForCompletedOrder_skipsWhenNotCompleted() {
        OrderRequest order = orderRequestRepository.save(OrderRequest.builder()
                .requestUser(em.find(User.class, requester.getId()))
                .status(OrderStatus.PARTIAL)
                .totalPrice(1000L)
                .requestDate(LocalDateTime.now())
                .build());

        deliveryService.createDeliveryIfAbsentForCompletedOrder(
                orderRequestRepository.findById(order.getId()).orElseThrow());

        assertThat(deliveryRepository.findByOrderRequest_Id(order.getId())).isEmpty();
    }

    @Test
    @DisplayName("배송 조회 성공 - 주문 ID로 상세 응답(감사 시각 포함)을 반환한다")
    void getDeliveryByOrderId_success() {
        OrderRequest order = orderRequestRepository.save(OrderRequest.builder()
                .requestUser(em.find(User.class, requester.getId()))
                .status(OrderStatus.APPROVED)
                .totalPrice(1000L)
                .requestDate(LocalDateTime.now())
                .receiverName("신청자")
                .receiverPhone("010-0000-0001")
                .destinationAddress("서울시 강남구 테헤란로 1")
                .build());
        deliveryRepository.save(Delivery.builder()
                .orderRequest(order)
                .receiverName("신청자")
                .receiverPhone("010-0000-0001")
                .destinationAddress("서울시 강남구 테헤란로 1")
                .build());

        DeliveryDto.DetailResponse response = deliveryService.getDeliveryByOrderId(order.getId());

        assertThat(response.getOrderId()).isEqualTo(order.getId());
        assertThat(response.getStatus()).isEqualTo(DeliveryStatus.READY);
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("배송 시작 성공 - READY에서 SHIPPING으로 바뀌고 운송장·발송일이 반영된다")
    void startShipping_success() {
        OrderRequest order = orderRequestRepository.save(OrderRequest.builder()
                .requestUser(em.find(User.class, requester.getId()))
                .status(OrderStatus.APPROVED)
                .totalPrice(1000L)
                .requestDate(LocalDateTime.now())
                .receiverName("신청자")
                .receiverPhone("010-0000-0001")
                .destinationAddress("서울시 강남구 테헤란로 1")
                .build());
        deliveryRepository.save(Delivery.builder()
                .orderRequest(order)
                .receiverName("신청자")
                .receiverPhone("010-0000-0001")
                .destinationAddress("주소")
                .build());

        LocalDateTime fixed = LocalDateTime.of(2026, 5, 1, 12, 0);
        DeliveryDto.StartShippingRequest req = DeliveryDto.StartShippingRequest.builder()
                .trackingNumber("TRK-001")
                .shippedAt(fixed)
                .build();

        DeliveryDto.DetailResponse response =
                deliveryService.startShipping(order.getId(), req);

        assertThat(response.getStatus()).isEqualTo(DeliveryStatus.SHIPPING);
        assertThat(response.getTrackingNumber()).isEqualTo("TRK-001");
        assertThat(response.getShippedAt()).isEqualTo(fixed);
    }

    @Test
    @DisplayName("배송 시작 - 권한 검증은 컨트롤러에서 처리되므로 서비스는 상태 검증만 수행한다")
    void startShipping_fail_invalidStatus() {
        OrderRequest order = orderRequestRepository.save(OrderRequest.builder()
                .requestUser(em.find(User.class, requester.getId()))
                .status(OrderStatus.APPROVED)
                .totalPrice(1000L)
                .requestDate(LocalDateTime.now())
                .receiverName("신청자")
                .receiverPhone("010-0000-0001")
                .destinationAddress("서울시 강남구 테헤란로 1")
                .build());

        Delivery delivery = deliveryRepository.save(Delivery.builder()
                .orderRequest(order)
                .receiverName("신청자")
                .receiverPhone("010-0000-0001")
                .destinationAddress("주소")
                .build());
        delivery.startShipping("TRK-0", LocalDateTime.now());
        deliveryRepository.saveAndFlush(delivery);
        em.clear();

        DeliveryDto.StartShippingRequest req =
                DeliveryDto.StartShippingRequest.builder().trackingNumber("TRK-9").build();

        assertThatThrownBy(() -> deliveryService.startShipping(order.getId(), req))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("배송 준비 상태에서만 배송 시작이 가능합니다.");
    }

    @Test
    @DisplayName("배송 완료 실패 - READY 상태면 INVALID_DELIVERY_STATUS")
    void completeDelivery_fail_invalidStatus() {
        OrderRequest order = orderRequestRepository.save(OrderRequest.builder()
                .requestUser(em.find(User.class, requester.getId()))
                .status(OrderStatus.APPROVED)
                .totalPrice(1000L)
                .requestDate(LocalDateTime.now())
                .receiverName("신청자")
                .receiverPhone("010-0000-0001")
                .destinationAddress("서울시 강남구 테헤란로 1")
                .build());
        deliveryRepository.save(Delivery.builder()
                .orderRequest(order)
                .receiverName("신청자")
                .receiverPhone("010-0000-0001")
                .destinationAddress("주소")
                .build());

        assertThatThrownBy(() -> deliveryService.completeDelivery(order.getId(), null))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("배송 중 상태에서만 배송 완료 처리가 가능합니다.");
    }

    @Test
    @DisplayName("배송 완료 성공 - SHIPPING에서 COMPLETED로 변경된다")
    void completeDelivery_success() {
        OrderRequest order = orderRequestRepository.save(OrderRequest.builder()
                .requestUser(em.find(User.class, requester.getId()))
                .status(OrderStatus.APPROVED)
                .totalPrice(1000L)
                .requestDate(LocalDateTime.now())
                .receiverName("신청자")
                .receiverPhone("010-0000-0001")
                .destinationAddress("서울시 강남구 테헤란로 1")
                .build());
        Delivery delivery = deliveryRepository.save(Delivery.builder()
                .orderRequest(order)
                .receiverName("신청자")
                .receiverPhone("010-0000-0001")
                .destinationAddress("주소")
                .build());
        delivery.startShipping("TRK-1", LocalDateTime.now());
        deliveryRepository.saveAndFlush(delivery);
        em.clear();

        LocalDateTime done = LocalDateTime.of(2026, 5, 2, 15, 30);
        DeliveryDto.CompleteDeliveryRequest req =
                DeliveryDto.CompleteDeliveryRequest.builder().deliveredAt(done).build();

        DeliveryDto.DetailResponse response =
                deliveryService.completeDelivery(order.getId(), req);

        assertThat(response.getStatus()).isEqualTo(DeliveryStatus.COMPLETED);
        assertThat(response.getDeliveredAt()).isEqualTo(done);
    }

    @Test
    @DisplayName("배송 생성 실패 - COMPLETED인데 발주 배송 스냅샷이 비어 있으면 예외")
    void createDeliveryIfAbsentForCompletedOrder_throwsWhenSnapshotMissing() {
        OrderRequest order = orderRequestRepository.save(OrderRequest.builder()
                .requestUser(em.find(User.class, requester.getId()))
                .status(OrderStatus.APPROVED)
                .totalPrice(1000L)
                .requestDate(LocalDateTime.now())
                .receiverName(null)
                .receiverPhone(null)
                .destinationAddress(null)
                .build());

        OrderRequest managed = orderRequestRepository.findById(order.getId()).orElseThrow();

        assertThatThrownBy(() -> deliveryService.createDeliveryIfAbsentForCompletedOrder(managed))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("발주에 저장된 배송 정보가 없습니다");
    }
}