package com.jstudy.inout.order.service;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.common.mail.config.MailComponent;
import com.jstudy.inout.order.entity.OrderDetail;
import com.jstudy.inout.order.entity.OrderDetailStatus;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.StockUsageHistory;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockUsageHistoryRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderApprovalTxService {

    private final OrderRequestRepository orderRequestRepository;
    private final StockUsageHistoryRepository usageHistoryRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final MailComponent mailComponent;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleOrderApproval(Long orderId, Long adminId) {
        OrderRequest order = orderRequestRepository.findById(orderId)
                .orElseThrow(() -> new InoutException("존재하지 않는 주문입니다.", 404, "ORDER_NOT_FOUND"));
        User adminUser = userRepository.findById(adminId)
                .orElseThrow(() -> new InoutException("관리자 정보를 찾을 수 없습니다.", 404, "ADMIN_NOT_FOUND"));

        for (OrderDetail detail : order.getOrderDetails()) {
            if (!detail.getStatus().isWaiting()) {
                continue;
            }
            approveItemStock(detail, adminUser, orderId);
            detail.updateStatus(OrderDetailStatus.APPROVED);
        }

        order.updateStatus(OrderStatus.COMPLETED);
        order.updateProcessDate(LocalDateTime.now());
        mailComponent.sendOrderStateEmail(order);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleOrderRejection(Long orderId, String reason) {
        OrderRequest order = orderRequestRepository.findById(orderId)
                .orElseThrow(() -> new InoutException("존재하지 않는 주문입니다.", 404, "ORDER_NOT_FOUND"));

        order.updateStatus(OrderStatus.REJECTED);
        order.updateRejectReason(reason);
        order.updateProcessDate(LocalDateTime.now());

        for (OrderDetail detail : order.getOrderDetails()) {
            detail.updateStatus(OrderDetailStatus.REJECTED);
        }

        mailComponent.sendOrderStateEmail(order);
    }

    private void approveItemStock(OrderDetail detail, User adminUser, Long orderId) {
        Item item = itemRepository.findByIdWithLock(detail.getItem().getItemId())
                .orElseThrow(() -> new InoutException("상품 정보가 없습니다.", 404, "ITEM_NOT_FOUND"));

        item.removeStock(detail.getRequestQuantity());

        StockUsageHistory usage = StockUsageHistory.builder()
                .item(item)
                .user(adminUser)
                .usageQuantity(detail.getRequestQuantity())
                .resultStock(item.getCurrentStock())
                .memo("발주 승인 (주문번호: " + orderId + ")")
                .build();
        usageHistoryRepository.save(usage);
    }
}
