package com.jstudy.inout.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.delivery.service.DeliveryService;
import com.jstudy.inout.order.event.OrderStateChangedEvent;
import com.jstudy.inout.order.dto.OrderProcessRequest;
import com.jstudy.inout.order.dto.OrderProcessRequest.ItemStatusUpdate;
import com.jstudy.inout.order.entity.OrderDetail;
import com.jstudy.inout.order.entity.OrderDetailStatus;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.OrderDetailRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockUsageHistoryRepository;

@ExtendWith(MockitoExtension.class)
class OrderAdmServiceTest {

    @InjectMocks
    private OrderAdmService orderAdmService;

    @Mock private OrderRequestRepository orderRequestRepository;
    @Mock private OrderDetailRepository orderDetailRepository;
    @Mock private StockUsageHistoryRepository usageHistoryRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderApprovalTxService orderApprovalTxService;
    @Mock private DeliveryService deliveryService;
    @Mock private EntityManager entityManager;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("상세 처리 실패 - 빈 요청 항목이면 예외가 발생한다")
    void processOrderItems_Fail_EmptyItems() {
        Long orderId = 100L;
        Long adminId = 1L;
        OrderRequest order = OrderRequest.builder().id(orderId).status(OrderStatus.ORDERED).build();
        User admin = User.builder().id(adminId).build();
        OrderProcessRequest request = new OrderProcessRequest(List.of());

        given(orderRequestRepository.findByIdForUpdate(orderId)).willReturn(Optional.of(order));
        given(userRepository.findById(adminId)).willReturn(Optional.of(admin));

        assertThatThrownBy(() -> orderAdmService.processOrderItems(orderId, request, adminId))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("처리할 발주 상세 항목이 없습니다.");

        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any(OrderStateChangedEvent.class));
    }

    @Test
    @DisplayName("상세 처리 실패 - 주문과 상세 소속이 일치하지 않으면 예외가 발생한다")
    void processOrderItems_Fail_DetailNotBelongToOrder() {
        Long orderId = 100L;
        Long adminId = 1L;
        Long orderDetailId = 999L;
        OrderRequest order = OrderRequest.builder().id(orderId).status(OrderStatus.ORDERED).build();
        User admin = User.builder().id(adminId).build();
        OrderProcessRequest request = new OrderProcessRequest(
                List.of(new ItemStatusUpdate(orderDetailId, OrderDetailStatus.APPROVED)));

        given(orderRequestRepository.findByIdForUpdate(orderId)).willReturn(Optional.of(order));
        given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
        given(orderDetailRepository.findByOrderDetailIdAndOrderRequest_Id(orderDetailId, orderId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> orderAdmService.processOrderItems(orderId, request, adminId))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("해당 주문에 속한 발주 상세 항목을 찾을 수 없습니다.");

        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any(OrderStateChangedEvent.class));
    }

    @Test
    @DisplayName("상세 처리 실패 - 이미 처리된 상태는 변경할 수 없다")
    void processOrderItems_Fail_InvalidStatusTransition() {
        Long orderId = 100L;
        Long adminId = 1L;
        Long orderDetailId = 10L;
        OrderRequest order = OrderRequest.builder().id(orderId).status(OrderStatus.ORDERED).build();
        User admin = User.builder().id(adminId).build();
        OrderDetail processedDetail = OrderDetail.builder()
                .orderDetailId(orderDetailId)
                .status(OrderDetailStatus.APPROVED)
                .build();
        OrderProcessRequest request = new OrderProcessRequest(
                List.of(new ItemStatusUpdate(orderDetailId, OrderDetailStatus.REJECTED)));

        given(orderRequestRepository.findByIdForUpdate(orderId)).willReturn(Optional.of(order));
        given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
        given(orderDetailRepository.findByOrderDetailIdAndOrderRequest_Id(orderDetailId, orderId))
                .willReturn(Optional.of(processedDetail));

        assertThatThrownBy(() -> orderAdmService.processOrderItems(orderId, request, adminId))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("이미 처리된 발주 상세 항목은 상태를 변경할 수 없습니다.");

        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any(OrderStateChangedEvent.class));
    }

    @Test
    @DisplayName("상세 처리 실패 - 대기 상태로 되돌릴 수 없다")
    void processOrderItems_Fail_CannotRollbackToWaiting() {
        Long orderId = 100L;
        Long adminId = 1L;
        Long orderDetailId = 11L;
        OrderRequest order = OrderRequest.builder().id(orderId).status(OrderStatus.ORDERED).build();
        User admin = User.builder().id(adminId).build();
        OrderDetail delayedDetail = OrderDetail.builder()
                .orderDetailId(orderDetailId)
                .status(OrderDetailStatus.DELAYED)
                .build();
        OrderProcessRequest request = new OrderProcessRequest(
                List.of(new ItemStatusUpdate(orderDetailId, OrderDetailStatus.WAITING)));

        given(orderRequestRepository.findByIdForUpdate(orderId)).willReturn(Optional.of(order));
        given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
        given(orderDetailRepository.findByOrderDetailIdAndOrderRequest_Id(orderDetailId, orderId))
                .willReturn(Optional.of(delayedDetail));

        assertThatThrownBy(() -> orderAdmService.processOrderItems(orderId, request, adminId))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("대기 상태로 되돌릴 수 없습니다.");

        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any(OrderStateChangedEvent.class));
    }

    @Test
    @DisplayName("AI 발주(모든 품목 AI) REQUESTED는 결제 검증을 우회하고 승인 완료된다")
    void processOrderItems_AiRequestedBypassNotPaidOrder() {
        Long orderId = 200L;
        Long adminId = 1L;
        Long orderDetailId = 20L;

        OrderDetail aiDetail = OrderDetail.builder()
                .orderDetailId(orderDetailId)
                .status(OrderDetailStatus.WAITING)
                .isAiSuggested(true)
                .build();

        OrderRequest aiOrder = OrderRequest.builder()
                .id(orderId)
                .status(OrderStatus.REQUESTED)
                .receiverName("미정")
                .receiverPhone("미정")
                .destinationAddress("미정 - 발주 확정 전 수정 필요")
                .orderDetails(new java.util.ArrayList<>(List.of(aiDetail)))
                .build();

        User admin = User.builder().id(adminId).build();
        OrderProcessRequest request = new OrderProcessRequest(
                List.of(new ItemStatusUpdate(orderDetailId, OrderDetailStatus.APPROVED)));

        given(orderRequestRepository.findByIdForUpdate(orderId)).willReturn(Optional.of(aiOrder));
        given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
        given(orderDetailRepository.findByOrderDetailIdAndOrderRequest_Id(orderDetailId, orderId))
                .willReturn(Optional.of(aiDetail));

        assertThatCode(() -> orderAdmService.processOrderItems(orderId, request, adminId))
                .doesNotThrowAnyException();

        assertThat(aiOrder.getStatus()).isEqualTo(OrderStatus.APPROVED);
        assertThat(aiDetail.getStatus()).isEqualTo(OrderDetailStatus.APPROVED);
        assertThat(aiOrder.getReceiverName()).isEqualTo("(주)본사지정협력사");
        verify(deliveryService).createDeliveryIfAbsentForCompletedOrder(aiOrder);
        verify(deliveryService).markAiInboundWaiting(orderId);
    }

    @Test
    @DisplayName("일반 발주(일부/전체 비AI) REQUESTED는 기존처럼 NOT_PAID_ORDER 예외가 발생한다")
    void processOrderItems_NormalRequestedStillFailsNotPaidOrder() {
        Long orderId = 201L;
        Long adminId = 1L;
        Long orderDetailId = 21L;

        OrderDetail normalDetail = OrderDetail.builder()
                .orderDetailId(orderDetailId)
                .status(OrderDetailStatus.WAITING)
                .isAiSuggested(false)
                .build();

        OrderRequest normalOrder = OrderRequest.builder()
                .id(orderId)
                .status(OrderStatus.REQUESTED)
                .orderDetails(new java.util.ArrayList<>(List.of(normalDetail)))
                .build();

        User admin = User.builder().id(adminId).build();
        OrderProcessRequest request = new OrderProcessRequest(
                List.of(new ItemStatusUpdate(orderDetailId, OrderDetailStatus.APPROVED)));

        given(orderRequestRepository.findByIdForUpdate(orderId)).willReturn(Optional.of(normalOrder));
        given(userRepository.findById(adminId)).willReturn(Optional.of(admin));

        assertThatThrownBy(() -> orderAdmService.processOrderItems(orderId, request, adminId))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("결제가 완료되지 않은 발주 건입니다.");
    }

    @Test
    @DisplayName("AI 발주 상세 조회 시 공급처/입고 연동 필드가 채워진다")
    void getOrderDetail_AiOrderContainsVendorAndInboundFields() {
        Long orderId = 202L;
        com.jstudy.inout.stock.entity.Item item = com.jstudy.inout.stock.entity.Item.builder()
                .itemId(300L)
                .name("테이크아웃 컵")
                .build();

        OrderDetail aiDetail = OrderDetail.builder()
                .orderDetailId(22L)
                .item(item)
                .status(OrderDetailStatus.APPROVED)
                .isAiSuggested(true)
                .aiReason("실재고 부족으로 자동 제안")
                .requestQuantity(5)
                .itemPriceSnapshot(1000L)
                .build();

        OrderRequest aiOrder = OrderRequest.builder()
                .id(orderId)
                .status(OrderStatus.APPROVED)
                .receiverName("미정")
                .receiverPhone("미정")
                .destinationAddress("미정")
                .requestDate(java.time.LocalDateTime.now().minusHours(2))
                .processDate(java.time.LocalDateTime.now().minusHours(1))
                .requestUser(User.builder().id(10L).name("관리자").build())
                .orderDetails(new java.util.ArrayList<>(List.of(aiDetail)))
                .build();

        given(orderRequestRepository.findByIdWithDetails(orderId)).willReturn(Optional.of(aiOrder));

        var detail = orderAdmService.getOrderDetail(orderId);

        assertThat(detail.isAiSuggestedOrder()).isTrue();
        assertThat(detail.getVendorName()).isEqualTo("(주)본사지정협력사");
        assertThat(detail.getInboundStatusLabel()).isEqualTo("승인 완료");
        assertThat(detail.getExpectedInboundAt()).isNotNull();
        assertThat(detail.getItems()).hasSize(1);
        assertThat(detail.getItems().get(0).getAiReason()).isEqualTo("실재고 부족으로 자동 제안");
    }
}
