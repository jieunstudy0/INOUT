package com.jstudy.inout.ai.service;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.dashboard.service.DashboardService;
import com.jstudy.inout.order.entity.OrderDetail;
import com.jstudy.inout.order.entity.OrderDetailStatus;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.stock.entity.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class AiAutoOrderPersistenceService {

    private static final String MEMO_PREFIX = "AI 자동 분석에 의해 생성된 발주 초안입니다.";

    private final OrderRequestRepository orderRequestRepository;
    private final UserRepository userRepository;
    private final DashboardService dashboardService;

    @Transactional
    public int saveDraftOrder(Map<Long, Item> itemMap,
            List<AiAutoOrderService.AiOrderRecommendation> recommendations) {

        List<AiAutoOrderService.AiOrderRecommendation> validRecs = new java.util.ArrayList<>();
        long totalPrice = 0L;
        for (AiAutoOrderService.AiOrderRecommendation rec : recommendations) {
            Item item = itemMap.get(rec.itemId());
            if (item == null) {
                log.warn("[AI 자동 발주] 추천된 itemId={}에 해당하는 품목 없음. 건너뜀.", rec.itemId());
                continue;
            }
            if (rec.recommendQty() <= 0) {
                log.warn("[AI 자동 발주] itemId={}의 추천 수량({})이 유효하지 않음. 건너뜀.",
                        rec.itemId(), rec.recommendQty());
                continue;
            }
            totalPrice += (long) rec.recommendQty() * item.getUnitPrice();
            validRecs.add(rec);
        }

        if (validRecs.isEmpty()) {
            log.warn("[AI 자동 발주] 유효한 추천 항목이 없어 발주 초안을 생성하지 않습니다.");
            return 0;
        }

        List<User> admins = userRepository.findAdminUsersSortedById(PageRequest.of(0, 1));
        if (admins.isEmpty()) {
            throw new InoutException("시스템 관리자 계정을 찾을 수 없습니다. DB에 ROLE_ADMIN 사용자가 필요합니다.",
                    500, "ADMIN_NOT_FOUND");
        }
        User adminUser = admins.get(0);

        Item firstItem = itemMap.get(validRecs.get(0).itemId());
        String representativeTitle = validRecs.size() > 1
                ? firstItem.getName() + " 외 " + (validRecs.size() - 1) + "건 (AI 자동 발주)"
                : firstItem.getName() + " (AI 자동 발주)";
        String memo = MEMO_PREFIX + " [" + representativeTitle + "]";

        OrderRequest order = OrderRequest.builder()
                .requestUser(adminUser)
                .status(OrderStatus.REQUESTED)
                .totalPrice(totalPrice)
                .requestDate(LocalDateTime.now())
                .memo(memo)
                .receiverName("(AI 초안)")
                .receiverPhone("미정")
                .destinationAddress("미정 - 발주 확정 전 수정 필요")
                .build();

        for (AiAutoOrderService.AiOrderRecommendation rec : validRecs) {
            Item item = itemMap.get(rec.itemId());
            OrderDetail detail = OrderDetail.builder()
                    .orderRequest(order)
                    .item(item)
                    .requestQuantity(rec.recommendQty())
                    .itemPriceSnapshot(item.getUnitPrice())
                    .status(OrderDetailStatus.WAITING)
                    .isAiSuggested(true)
                    .aiReason(rec.reason())
                    .build();
            order.getOrderDetails().add(detail);
        }

        orderRequestRepository.save(order);
        safeEvictDashboardSummary();
        log.info("[AI 자동 발주] 통합 발주 초안 1건 저장 완료 (품목 {}개, 합계 {}원)",
                validRecs.size(), String.format("%,d", totalPrice));
        return 1;
    }

    /**
     * Redis 장애 시 캐시 무효화 실패가 발주 초안 저장 트랜잭션을 롤백하지 않도록 격리한다.
     */
    private void safeEvictDashboardSummary() {
        try {
            dashboardService.evictDashboardSummary();
        } catch (RuntimeException ex) {
            log.warn("[AI 자동 발주] 대시보드 캐시 무효화 실패(무시). Redis 미가용해도 발주 초안은 유지됩니다. cause={}",
                    ex.getMessage());
        }
    }
}
