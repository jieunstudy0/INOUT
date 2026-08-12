package com.jstudy.inout.order.service;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.auth.util.UserDisplayNames;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.order.dto.OrderAdminDetailResponse;
import com.jstudy.inout.order.dto.OrderAdminResponse;
import com.jstudy.inout.order.dto.OrderRejectRequest;
import com.jstudy.inout.order.dto.OwnerOrderCreateRequest;
import com.jstudy.inout.order.dto.OwnerOrderModifyRequest;
import com.jstudy.inout.order.entity.OrderDetail;
import com.jstudy.inout.order.entity.OrderDetailStatus;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.OrderDetailRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.payment.service.DepositService;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.repository.ItemRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderOwnerService {

    private final OrderRequestRepository orderRequestRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final DepositService depositService;

    public Page<OrderAdminResponse> getStoreOrders(Long ownerUserId, OrderStatus status, Pageable pageable) {
        Long storeId = requireStoreId(ownerUserId);
        Page<OrderRequest> page = (status == null)
                ? orderRequestRepository.findByStoreIdOrderByRequestDateDesc(storeId, pageable)
                : orderRequestRepository.findByStoreIdAndStatusOrderByRequestDateDesc(storeId, status, pageable);
        return page.map(this::toListItem);
    }

    public OrderAdminDetailResponse getStoreOrderDetail(Long ownerUserId, Long orderId) {
        OrderRequest order = requireStoreOrder(ownerUserId, orderId);
        return toDetail(order);
    }

    /**
     * 직원 기안(REQUESTED) 수정 후 예치금 결제 → ORDERED
     */
    @Transactional
    public Long modifyAndApprove(Long ownerUserId, Long orderId, OwnerOrderModifyRequest request) {
        User owner = requireOwner(ownerUserId);
        OrderRequest order = orderRequestRepository.findByIdForUpdateWithDetails(orderId)
                .orElseThrow(() -> new InoutException("존재하지 않는 주문입니다.", 404, "ORDER_NOT_FOUND"));
        assertSameStore(owner, order);

        if (order.getStatus() != OrderStatus.REQUESTED) {
            throw new InoutException("직원 기안(승인 대기) 상태의 발주만 수정·결제할 수 있습니다.", 400, "INVALID_ORDER_STATUS");
        }
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new InoutException("결제할 품목이 없습니다.", 400, "EMPTY_ORDER_ITEMS");
        }

        Map<Long, Integer> qtyByItemId = new HashMap<>();
        for (OwnerOrderModifyRequest.ItemLine line : request.getItems()) {
            if (line.getItemId() == null || line.getQuantity() == null || line.getQuantity() < 1) {
                throw new InoutException("품목 ID와 수량(1 이상)이 필요합니다.", 400, "INVALID_ITEM_LINE");
            }
            qtyByItemId.merge(line.getItemId(), line.getQuantity(), Integer::sum);
        }

        Set<Long> keepItemIds = new HashSet<>(qtyByItemId.keySet());
        Iterator<OrderDetail> it = order.getOrderDetails().iterator();
        while (it.hasNext()) {
            OrderDetail detail = it.next();
            Long itemId = detail.getItem().getItemId();
            if (!keepItemIds.contains(itemId)) {
                it.remove();
                continue;
            }
            detail.updateQuantity(qtyByItemId.get(itemId));
            keepItemIds.remove(itemId);
        }

        // 요청에만 있고 기존에 없던 품목은 신규 추가
        for (Long itemId : keepItemIds) {
            Item item = itemRepository.findById(itemId)
                    .orElseThrow(() -> new InoutException("상품을 찾을 수 없습니다. (id=" + itemId + ")", 404, "ITEM_NOT_FOUND"));
            OrderDetail detail = OrderDetail.builder()
                    .orderRequest(order)
                    .item(item)
                    .requestQuantity(qtyByItemId.get(itemId))
                    .itemPriceSnapshot(item.getUnitPrice())
                    .status(OrderDetailStatus.WAITING)
                    .build();
            order.getOrderDetails().add(detail);
            orderDetailRepository.save(detail);
        }

        if (order.getOrderDetails().isEmpty()) {
            throw new InoutException("결제할 품목이 없습니다.", 400, "EMPTY_ORDER_ITEMS");
        }

        long newTotal = order.getOrderDetails().stream()
                .mapToLong(d -> d.getItemPriceSnapshot() * d.getRequestQuantity())
                .sum();
        order.updateTotalPrice(newTotal);

        Long payerUserId = order.getRequestUser().getId();
        User payer = userRepository.findByIdForUpdate(payerUserId)
                .orElseThrow(() -> new InoutException("신청자 정보를 찾을 수 없습니다.", 404, "USER_NOT_FOUND"));
        try {
            payer.consumeDailyDeposit(newTotal);
        } catch (IllegalStateException e) {
            throw new InoutException("1일 예치금 사용 한도를 초과했습니다", 400, "DAILY_DEPOSIT_LIMIT_EXCEEDED");
        }

        depositService.deductDeposit(
                payerUserId,
                ownerUserId,
                newTotal,
                "점주 승인 결제 — 발주 #" + order.getId(),
                order.getId());

        order.updateStatus(OrderStatus.ORDERED);
        order.updateProcessDate(LocalDateTime.now());
        order.assignProcessUser(owner);
        return order.getId();
    }

    /**
     * 직원 기안 반려 (미결제 → 예치금 환불 없음)
     */
    @Transactional
    public void rejectDraft(Long ownerUserId, Long orderId, OrderRejectRequest request) {
        User owner = requireOwner(ownerUserId);
        OrderRequest order = orderRequestRepository.findByIdForUpdateWithDetails(orderId)
                .orElseThrow(() -> new InoutException("존재하지 않는 주문입니다.", 404, "ORDER_NOT_FOUND"));
        assertSameStore(owner, order);

        if (order.getStatus() != OrderStatus.REQUESTED) {
            throw new InoutException("직원 기안(승인 대기) 상태의 발주만 반려할 수 있습니다.", 400, "INVALID_ORDER_STATUS");
        }

        String reason = (request != null && StringUtils.hasText(request.getReason()))
                ? request.getReason().trim()
                : "점주 기안 반려";
        order.updateStatus(OrderStatus.REJECTED);
        order.updateRejectReason(reason);
        order.updateProcessDate(LocalDateTime.now());
        order.assignProcessUser(owner);
        for (OrderDetail detail : order.getOrderDetails()) {
            detail.updateStatus(OrderDetailStatus.REJECTED);
        }
    }

    /**
     * 점주 직접 발주 — 즉시 예치금 차감 후 ORDERED
     */
    @Transactional
    public Long createOwnerOrder(Long ownerUserId, OwnerOrderCreateRequest request) {
        User owner = requireOwner(ownerUserId);
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new InoutException("발주할 품목이 없습니다.", 400, "EMPTY_ORDER");
        }

        Map<Long, Integer> qtyByItemId = new HashMap<>();
        for (OwnerOrderCreateRequest.ItemLine line : request.getItems()) {
            if (line.getItemId() == null || line.getQuantity() == null || line.getQuantity() < 1) {
                throw new InoutException("품목 ID와 수량(1 이상)이 필요합니다.", 400, "INVALID_ITEM_LINE");
            }
            qtyByItemId.merge(line.getItemId(), line.getQuantity(), Integer::sum);
        }

        long total = 0L;
        Map<Long, Item> items = new HashMap<>();
        for (Map.Entry<Long, Integer> e : qtyByItemId.entrySet()) {
            Item item = itemRepository.findByIdWithLock(e.getKey())
                    .orElseThrow(() -> new InoutException("상품을 찾을 수 없습니다.", 404, "ITEM_NOT_FOUND"));
            if (item.getCurrentStock() < e.getValue()) {
                throw new InoutException(item.getName() + " 재고가 부족합니다.", 400, "STOCK_SHORTAGE");
            }
            items.put(e.getKey(), item);
            total += item.getUnitPrice() * e.getValue();
        }

        User lockedOwner = userRepository.findByIdForUpdate(ownerUserId)
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404, "USER_NOT_FOUND"));
        try {
            lockedOwner.consumeDailyDeposit(total);
        } catch (IllegalStateException e) {
            throw new InoutException("1일 예치금 사용 한도를 초과했습니다", 400, "DAILY_DEPOSIT_LIMIT_EXCEEDED");
        }

        OrderRequest order = OrderRequest.builder()
                .requestUser(lockedOwner)
                .status(OrderStatus.ORDERED)
                .totalPrice(total)
                .requestDate(LocalDateTime.now())
                .processDate(LocalDateTime.now())
                .processUser(lockedOwner)
                .receiverName(resolveReceiverName(request, lockedOwner))
                .receiverPhone(resolveReceiverPhone(request, lockedOwner))
                .destinationAddress(resolveDestinationAddress(request, lockedOwner))
                .build();
        orderRequestRepository.save(order);

        for (Map.Entry<Long, Integer> e : qtyByItemId.entrySet()) {
            Item item = items.get(e.getKey());
            OrderDetail detail = OrderDetail.builder()
                    .orderRequest(order)
                    .item(item)
                    .requestQuantity(e.getValue())
                    .itemPriceSnapshot(item.getUnitPrice())
                    .status(OrderDetailStatus.WAITING)
                    .build();
            orderDetailRepository.save(detail);
        }

        depositService.deductDeposit(
                ownerUserId,
                ownerUserId,
                total,
                "점주 직접 발주 결제 — 발주 #" + order.getId(),
                order.getId());

        return order.getId();
    }

    private OrderRequest requireStoreOrder(Long ownerUserId, Long orderId) {
        Long storeId = requireStoreId(ownerUserId);
        OrderRequest order = orderRequestRepository.findWithDetailsGraphById(orderId)
                .orElseThrow(() -> new InoutException("존재하지 않는 주문입니다.", 404, "ORDER_NOT_FOUND"));
        if (order.getRequestUser().getStore() == null
                || !order.getRequestUser().getStore().getId().equals(storeId)) {
            throw new InoutException("소속 매장 발주만 조회할 수 있습니다.", 403, "FORBIDDEN");
        }
        return order;
    }

    private void assertSameStore(User owner, OrderRequest order) {
        if (owner.getStore() == null || order.getRequestUser().getStore() == null
                || !Objects.equals(owner.getStore().getId(), order.getRequestUser().getStore().getId())) {
            throw new InoutException("소속 매장 발주만 처리할 수 있습니다.", 403, "FORBIDDEN");
        }
    }

    private OrderAdminDetailResponse toDetail(OrderRequest order) {
        List<OrderAdminDetailResponse.ItemDto> items =
                (order.getOrderDetails() == null ? List.<OrderDetail>of() : order.getOrderDetails())
                        .stream()
                        .map(d -> OrderAdminDetailResponse.ItemDto.builder()
                                .orderDetailId(d.getOrderDetailId())
                                .itemId(d.getItem().getItemId())
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
                .storeName(UserDisplayNames.storeName(order.getRequestUser()))
                .employeeName(UserDisplayNames.displayName(order.getRequestUser()))
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
                .storeName(UserDisplayNames.storeName(order.getRequestUser()))
                .employeeName(UserDisplayNames.displayName(order.getRequestUser()))
                .requestDate(order.getRequestDate())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .representativeItemName(repItemName)
                .itemCount(itemCount)
                .build();
    }

    private User requireOwner(Long ownerUserId) {
        User owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404, "USER_NOT_FOUND"));
        if (owner.getStore() == null) {
            throw new InoutException("소속 매장 정보가 없습니다.", 403, "STORE_REQUIRED");
        }
        return owner;
    }

    private Long requireStoreId(Long ownerUserId) {
        return requireOwner(ownerUserId).getStore().getId();
    }

    private String resolveReceiverName(OwnerOrderCreateRequest request, User user) {
        if (StringUtils.hasText(request.getReceiverName())) return request.getReceiverName().trim();
        return UserDisplayNames.displayName(user);
    }

    private String resolveReceiverPhone(OwnerOrderCreateRequest request, User user) {
        if (StringUtils.hasText(request.getReceiverPhone())) return request.getReceiverPhone().trim();
        return user.getPhone() != null ? user.getPhone() : "";
    }

    private String resolveDestinationAddress(OwnerOrderCreateRequest request, User user) {
        if (StringUtils.hasText(request.getDestinationAddress())) return request.getDestinationAddress().trim();
        if (user.getStore() != null && StringUtils.hasText(user.getStore().getAddress())) {
            return user.getStore().getAddress().trim();
        }
        return "";
    }
}
