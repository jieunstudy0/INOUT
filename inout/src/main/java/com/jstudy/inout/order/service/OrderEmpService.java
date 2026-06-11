package com.jstudy.inout.order.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.order.dto.OrderCreateRequest;
import com.jstudy.inout.order.dto.OrderDetailResponse;
import com.jstudy.inout.order.dto.OrderListResponse;
import com.jstudy.inout.order.dto.OrderPreResponse;
import com.jstudy.inout.order.entity.CartDetail;
import com.jstudy.inout.order.entity.OrderDetail;
import com.jstudy.inout.order.entity.OrderDetailStatus;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.CartDetailRepository;
import com.jstudy.inout.order.repository.OrderDetailRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.payment.dto.DepositDto;
import com.jstudy.inout.payment.service.DepositService;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.repository.ItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderEmpService {
        
    private final CartDetailRepository cartDetailRepository;
    private final UserRepository userRepository;
    private final OrderRequestRepository orderRequestRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ItemRepository itemRepository;
    private final DepositService depositService;

    @Transactional(readOnly = true) 
    public OrderPreResponse getOrderPreview(Long userId, OrderCreateRequest request) {

        List<CartDetail> selectedItems = cartDetailRepository.findWithCartAndUserByIds(request.getCartDetailIds());

        if (selectedItems.isEmpty()) {
            throw new InoutException("선택된 상품이 없습니다.", 400, "EMPTY_SELECTION");
        }

        User user = userRepository.findById(userId).orElseThrow();

        for (CartDetail detail : selectedItems) {
            if (!detail.getCart().getUser().getId().equals(userId)) {
                throw new InoutException("본인의 장바구니 상품만 주문할 수 있습니다.", 403, "FORBIDDEN");
            }
        }
        return OrderPreResponse.from(user, selectedItems);
    }

    @Transactional
    public Long submitOrderRequest(Long userId, OrderCreateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404, "USER_NOT_FOUND"));

        List<CartDetail> selectedItems = cartDetailRepository.findWithCartAndUserByIds(request.getCartDetailIds());

        if (selectedItems.isEmpty()) {
            throw new InoutException("발주할 상품이 없습니다.", 400, "EMPTY_ORDER");
        }

        for (CartDetail cartItem : selectedItems) {
            if (!cartItem.getCart().getUser().getId().equals(userId)) {
                throw new InoutException("본인의 장바구니 상품만 주문할 수 있습니다.", 403, "FORBIDDEN");
            }

            Item item = itemRepository.findByIdWithLock(cartItem.getItem().getItemId())
                    .orElseThrow(() -> new InoutException("상품을 찾을 수 없습니다.", 404, "ITEM_NOT_FOUND"));
            
            if (item.getCurrentStock() < cartItem.getQuantity()) {
                throw new InoutException(item.getName() + " 재고가 부족합니다.", 400, "STOCK_SHORTAGE");
            }
        }

        long calculatedTotalPrice = selectedItems.stream()
                .mapToLong(cd -> cd.getItem().getUnitPrice() * cd.getQuantity())
                .sum();

        OrderRequest orderRequest = OrderRequest.builder()
                .requestUser(user)
                .status(OrderStatus.REQUESTED)
                .totalPrice(calculatedTotalPrice)
                .requestDate(LocalDateTime.now())
                .receiverName(resolveReceiverName(request, user))
                .receiverPhone(resolveReceiverPhone(request, user))
                .destinationAddress(resolveDestinationAddress(request, user))
                .build();
        
        orderRequestRepository.save(orderRequest);

        for (CartDetail cartItem : selectedItems) {
            OrderDetail detail = OrderDetail.builder()
                    .orderRequest(orderRequest)
                    .item(cartItem.getItem())
                    .requestQuantity(cartItem.getQuantity())
                    .itemPriceSnapshot(cartItem.getItem().getUnitPrice())
                    .status(OrderDetailStatus.WAITING)
                    .build();
            orderDetailRepository.save(detail);
        }

        cartDetailRepository.updateDeletedStatusInBatch(request.getCartDetailIds());
        
        return orderRequest.getId();
    }

    @Transactional(readOnly = true)
    public List<OrderListResponse> getMyOrderHistory(Long userId) {

        List<OrderRequest> orders = orderRequestRepository.findAllByRequestUser_IdOrderByRequestDateDesc(userId);

        return orders.stream()
                .filter(order -> order.getStatus() != OrderStatus.REQUESTED)
                .<OrderListResponse>map(order -> {
                    String repName = order.getOrderDetails().isEmpty() ? "상품 없음" 
                            : order.getOrderDetails().get(0).getItem().getName();
                    int extraCount = order.getOrderDetails().size() - 1;
                    String displayName = extraCount > 0 ? repName + " 외 " + extraCount + "건" : repName;

                    return OrderListResponse.builder()
                            .orderRequestId(order.getId())
                            .requestDate(order.getRequestDate())
                            .status(order.getStatus())
                            .totalPrice(order.getTotalPrice())
                            .itemCount(order.getOrderDetails().size())
                            .representativeItemName(displayName)
                            .build();
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetails(Long userId, Long orderRequestId) {

        OrderRequest order = orderRequestRepository.findWithDetailsGraphById(orderRequestId)
                .orElseThrow(() -> new InoutException("존재하지 않는 발주 내역입니다.", 404, "ORDER_NOT_FOUND"));

        if (!order.getRequestUser().getId().equals(userId)) {
            throw new InoutException("조회 권한이 없습니다.", 403, "FORBIDDEN");
        }

        List<OrderDetailResponse.OrderDetailItemDto> itemDtos = order.getOrderDetails().stream()
                .map(detail -> OrderDetailResponse.OrderDetailItemDto.builder()
                        .itemName(detail.getItem().getName())
                        .quantity(detail.getRequestQuantity())
                        .priceSnapshot(detail.getItemPriceSnapshot())
                        .subTotal((long) detail.getRequestQuantity() * detail.getItemPriceSnapshot())
                        .build())
                .collect(Collectors.toList());

        return OrderDetailResponse.builder()
                .orderRequestId(order.getId())
                .requestDate(order.getRequestDate())
                .status(order.getStatus())
                .storeName(order.getRequestUser().getStore().getName())
                .employeeName(order.getRequestUser().getName())
                .totalPrice(order.getTotalPrice())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .destinationAddress(order.getDestinationAddress())
                .items(itemDtos)
                .build();
    }
    
    @Transactional
    public void cancelOrder(Long userId, Long orderRequestId) {

        OrderRequest order = orderRequestRepository.findByIdForUpdate(orderRequestId)
                .orElseThrow(() -> new InoutException("주문 정보를 찾을 수 없습니다.", 404, "ORDER_NOT_FOUND"));

        if (!order.getRequestUser().getId().equals(userId)) {
            throw new InoutException("본인의 주문만 취소할 수 있습니다.", 403, "FORBIDDEN");
        }

        OrderStatus currentStatus = order.getStatus();

        if (currentStatus == OrderStatus.REQUESTED) {
            order.updateStatus(OrderStatus.CANCELLED);
            order.updateProcessDate(LocalDateTime.now());

        } else if (currentStatus == OrderStatus.PAID) {
            DepositDto.RefundRequest refundRequest = DepositDto.RefundRequest.builder()
                    .amount(order.getTotalPrice())
                    .description("발주 취소 환불 (주문번호: #" + orderRequestId + ")")
                    .build();
            depositService.refundDeposit(userId, userId, refundRequest);

            order.updateStatus(OrderStatus.CANCELLED);
            order.updateProcessDate(LocalDateTime.now());

        } else {
            throw new InoutException("이미 처리 진행 중이거나 완료된 주문은 취소할 수 없습니다.", 400, "INVALID_STATUS");
        }
    }

    private String resolveReceiverName(OrderCreateRequest request, User user) {
        return StringUtils.hasText(request.getReceiverName()) ? request.getReceiverName().trim() : user.getName();
    }

    private String resolveReceiverPhone(OrderCreateRequest request, User user) {
        return StringUtils.hasText(request.getReceiverPhone()) ? request.getReceiverPhone().trim() : user.getPhone();
    }

    private String resolveDestinationAddress(OrderCreateRequest request, User user) {
        if (StringUtils.hasText(request.getDestinationAddress())) {
            return request.getDestinationAddress().trim();
        }
        if (user.getStore() != null && StringUtils.hasText(user.getStore().getAddress())) {
            return user.getStore().getAddress().trim();
        }
        return "(매장 주소 미등록)";
    }
}