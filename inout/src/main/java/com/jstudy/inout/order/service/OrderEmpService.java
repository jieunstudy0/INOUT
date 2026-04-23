package com.jstudy.inout.order.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderEmpService {
		
	private final CartDetailRepository cartDetailRepository;
	private final UserRepository userRepository;
	private final OrderRequestRepository orderRequestRepository;
    private final OrderDetailRepository orderDetailRepository;

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
    public void submitOrderRequest(Long userId, OrderCreateRequest request) {
    	
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404, "USER_NOT_FOUND"));

        List<CartDetail> selectedItems = cartDetailRepository.findAllById(request.getCartDetailIds());
        
        if (selectedItems.isEmpty()) {
            throw new InoutException("발주할 상품이 없습니다.", 400, "EMPTY_ORDER");
        }

        long calculatedTotalPrice = selectedItems.stream()
                .mapToLong(cd -> cd.getItem().getUnitPrice() * cd.getQuantity())
                .sum();

        OrderRequest orderRequest = OrderRequest.builder()
                .requestUser(user)
                .status(OrderStatus.REQUESTED)
                .totalPrice(calculatedTotalPrice)
                .requestDate(LocalDateTime.now())
                .build();
        
        orderRequestRepository.save(orderRequest);

        for (CartDetail cartItem : selectedItems) {
            OrderDetail detail = OrderDetail.builder()
                    .orderRequest(orderRequest)
                    .item(cartItem.getItem())
                    .requestQuantity(cartItem.getQuantity())
                    .itemPriceSnapshot(cartItem.getItem().getUnitPrice()) // 발주 시점 단가 저장
                    .status(OrderDetailStatus.WAITING)
                    .build();
            orderDetailRepository.save(detail);
        }

        cartDetailRepository.updateDeletedStatusInBatch(request.getCartDetailIds());
    }

    @Transactional(readOnly = true)
    public List<OrderListResponse> getMyOrderHistory(Long userId) {

        List<OrderRequest> orders = orderRequestRepository.findAllByRequestUser_UserIdOrderByRequestDateDesc(userId);

        return orders.stream()
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

        OrderRequest order = orderRequestRepository.findById(orderRequestId)
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
                .items(itemDtos)
                .totalPrice(order.getTotalPrice())
                .build();
    }
    
    @Transactional
    public void cancelOrder(Long userId, Long orderRequestId) {

        OrderRequest order = orderRequestRepository.findById(orderRequestId)
                .orElseThrow(() -> new InoutException("주문 정보를 찾을 수 없습니다.", 404, "ORDER_NOT_FOUND"));

        if (!order.getRequestUser().getId().equals(userId)) {
            throw new InoutException("본인의 주문만 취소할 수 있습니다.", 403, "FORBIDDEN");
        }

        if (order.getStatus() != OrderStatus.REQUESTED) {
            throw new InoutException("이미 처리 진행 중이거나 완료된 주문은 취소할 수 없습니다.", 400, "INVALID_STATUS");
        }

        order.updateStatus(OrderStatus.CANCELLED);
        order.updateProcessDate(LocalDateTime.now());
    }   	
}
