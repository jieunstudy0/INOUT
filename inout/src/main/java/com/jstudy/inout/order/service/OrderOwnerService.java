package com.jstudy.inout.order.service;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.order.dto.OrderAdminDetailResponse;
import com.jstudy.inout.order.dto.OrderAdminResponse;
import com.jstudy.inout.order.entity.OrderDetail;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderOwnerService {

    private final OrderRequestRepository orderRequestRepository;
    private final UserRepository userRepository;

    public Page<OrderAdminResponse> getStoreOrders(Long ownerUserId, OrderStatus status, Pageable pageable) {
        Long storeId = requireStoreId(ownerUserId);
        Page<OrderRequest> page = (status == null)
                ? orderRequestRepository.findByStoreIdOrderByRequestDateDesc(storeId, pageable)
                : orderRequestRepository.findByStoreIdAndStatusOrderByRequestDateDesc(storeId, status, pageable);
        return page.map(this::toListItem);
    }

    public OrderAdminDetailResponse getStoreOrderDetail(Long ownerUserId, Long orderId) {
        Long storeId = requireStoreId(ownerUserId);
        OrderRequest order = orderRequestRepository.findWithDetailsGraphById(orderId)
                .orElseThrow(() -> new InoutException("존재하지 않는 주문입니다.", 404, "ORDER_NOT_FOUND"));

        if (order.getRequestUser().getStore() == null
                || !order.getRequestUser().getStore().getId().equals(storeId)) {
            throw new InoutException("소속 매장 발주만 조회할 수 있습니다.", 403, "FORBIDDEN");
        }

        List<OrderAdminDetailResponse.ItemDto> items =
                (order.getOrderDetails() == null ? List.<OrderDetail>of() : order.getOrderDetails())
                        .stream()
                        .map(d -> OrderAdminDetailResponse.ItemDto.builder()
                                .orderDetailId(d.getOrderDetailId())
                                .itemName(d.getItem().getName())
                                .quantity(d.getRequestQuantity())
                                .priceSnapshot(d.getItemPriceSnapshot())
                                .subTotal(d.getItemPriceSnapshot() != null
                                        ? d.getItemPriceSnapshot() * d.getRequestQuantity() : 0L)
                                .status(d.getStatus())
                                .isAiSuggested(d.isAiSuggested())
                                .aiReason(d.getAiReason())
                                .build())
                        .collect(Collectors.toList());

        return OrderAdminDetailResponse.builder()
                .orderRequestId(order.getId())
                .requestDate(order.getRequestDate())
                .status(order.getStatus())
                .storeName(order.getRequestUser().getStore().getName())
                .employeeName(order.getRequestUser().getName())
                .totalPrice(order.getTotalPrice())
                .rejectReason(order.getRejectReason())
                .items(items)
                .build();
    }

    private OrderAdminResponse toListItem(OrderRequest order) {
        String repItemName = "상품 없음";
        int itemCount = 0;
        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            repItemName = order.getOrderDetails().get(0).getItem().getName();
            itemCount = order.getOrderDetails().size();
        }
        return OrderAdminResponse.builder()
                .orderRequestId(order.getId())
                .storeName(order.getRequestUser().getStore() != null
                        ? order.getRequestUser().getStore().getName() : "-")
                .employeeName(order.getRequestUser().getName())
                .requestDate(order.getRequestDate())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .representativeItemName(repItemName)
                .itemCount(itemCount)
                .build();
    }

    private Long requireStoreId(Long ownerUserId) {
        User owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404, "USER_NOT_FOUND"));
        if (owner.getStore() == null) {
            throw new InoutException("소속 매장 정보가 없습니다.", 403, "STORE_REQUIRED");
        }
        return owner.getStore().getId();
    }
}
