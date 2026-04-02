package com.jstudy.inout.order.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.order.dto.CartAddRequest;
import com.jstudy.inout.order.dto.CartResponse;
import com.jstudy.inout.order.entity.Cart;
import com.jstudy.inout.order.entity.CartDetail;
import com.jstudy.inout.order.entity.OrderDetail;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.repository.CartDetailRepository;
import com.jstudy.inout.order.repository.CartRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.repository.ItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CartEmpService {
	
	private final CartDetailRepository cartDetailRepository;
	private final UserRepository userRepository;
	private final CartRepository cartRepository;
	private final ItemRepository itemRepository;
	private final OrderRequestRepository orderRequestRepository;
	
    @Transactional
    public void addToCart(Long userId, CartAddRequest request) {
    	
        Cart cart = getOrCreateCart(userId);

        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new InoutException("상품을 찾을 수 없습니다.", 404, "ITEM_NOT_FOUND"));

        addItemToCartInternal(cart, item, request.getQuantity());
    }

    @Transactional
    public void reOrder(Long userId, Long pastOrderId) {

        OrderRequest pastOrder = orderRequestRepository.findById(pastOrderId)
                .orElseThrow(() -> new InoutException("존재하지 않는 주문입니다.", 404, "ORDER_NOT_FOUND"));

        if (!pastOrder.getRequestUser().getId().equals(userId)) {
            throw new InoutException("본인의 주문 내역만 재주문할 수 있습니다.", 403, "FORBIDDEN");
        }

        Cart cart = getOrCreateCart(userId);

        for (OrderDetail detail : pastOrder.getOrderDetails()) {
            Item item = detail.getItem();
            if (item.getDeleted() || item.getCurrentStock() == 0) {
                continue; 
            }
            addItemToCartInternal(cart, item, detail.getRequestQuantity());
        }
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUser_UserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder()
                		.user(userRepository.findById(userId)
                			    .orElseThrow(() -> new InoutException("사용자 없음", 404, "USER_NOT_FOUND")))
                        .build()));
    }

    private void addItemToCartInternal(Cart cart, Item item, int quantity) {
        Optional<CartDetail> existingDetail = cartDetailRepository.findByCartAndItem(cart, item);

        if (existingDetail.isPresent()) {
            existingDetail.get().updateQuantity(existingDetail.get().getQuantity() + quantity);
        } else {
            CartDetail cartDetail = CartDetail.builder()
                    .cart(cart)
                    .item(item)
                    .quantity(quantity)
                    .build();
            cartDetailRepository.save(cartDetail);
        }
    }
	
	@Transactional(readOnly = true)
	public CartResponse getCartList(Long userId) {
		
		List<CartDetail> cartDetails = cartDetailRepository.findAllByCart_User_UserId(userId);

	    List<CartResponse.CartItemResponse> itemResponses = cartDetails.stream()
	    	    .map(CartResponse.CartItemResponse::from) 
	    	    .collect(Collectors.toList());

	    int totalQuantity = itemResponses.stream().mapToInt(it -> it.getQuantity()).sum();
	    long totalPrice = itemResponses.stream().mapToLong(it -> it.getSubTotal()).sum();

	    return CartResponse.builder()
	            .items(itemResponses)
	            .totalQuantity(totalQuantity)
	            .totalPrice(totalPrice)
	            .build();
	}
	
	@Transactional
	public void deleteSelectedCartItems(Long userId, List<Long> cartDetailIds) {
	    if (cartDetailIds == null || cartDetailIds.isEmpty()) return;

	    cartDetailRepository.updateDeletedStatusInBatch(cartDetailIds);
	}

	@Transactional
	public void deleteAllCartItems(Long userId) {
	    cartDetailRepository.updateAllDeletedStatusByUserId(userId);
	}
	
    @Transactional
    public void updateQuantity(Long userId, Long cartDetailId, int newQuantity) {

        CartDetail cartDetail = cartDetailRepository.findById(cartDetailId)
                .orElseThrow(() -> new InoutException("항목을 찾을 수 없습니다.", 404, "NOT_FOUND"));

        if (!cartDetail.getCart().getUser().getId().equals(userId)) {
            throw new InoutException("수정 권한이 없습니다.", 403, "FORBIDDEN");
        }
        cartDetail.updateQuantity(newQuantity);
    }
}
