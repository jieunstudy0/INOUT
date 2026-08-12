package com.jstudy.inout.stock.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.stock.dto.emp.AiStockSuggestionResponse;
import com.jstudy.inout.stock.dto.emp.ItemResponse;
import com.jstudy.inout.stock.dto.emp.StockHistoryResponse;
import com.jstudy.inout.stock.dto.emp.StockUserDetailResponse;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.StockReceivingHistory;
import com.jstudy.inout.stock.entity.StockUsageHistory;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockReceivingHistoryRepository;
import com.jstudy.inout.stock.repository.StockUsageHistoryRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class StockEmpService {

    private final ItemRepository itemRepository;
    private final StockUsageHistoryRepository usageHistoryRepository;
    private final UserRepository userRepository;
    private final StockReceivingHistoryRepository receivingHistoryRepository;

    @Transactional
    public Long useStock(Long itemId, int quantity, Long userId, String memo) {

        Item item = itemRepository.findByIdWithLock(itemId)
                .filter(i -> !i.getDeleted())
                .orElseThrow(() -> new InoutException("사용 가능한 상품이 없습니다.", 404, "ITEM_NOT_FOUND"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404, "USER_NOT_FOUND"));

        item.removeStock(quantity);

        StockUsageHistory history = StockUsageHistory.builder()
                .item(item)
                .user(user)
                .usageQuantity(quantity)
                .resultStock(item.getCurrentStock()) 
                .memo(memo)
                .build();

        usageHistoryRepository.save(history);
        return item.getItemId();
    }
    
    
    @Transactional(readOnly = true)
    public Page<ItemResponse> getEmployeeStockList(String name, Pageable pageable) {
        Page<Item> stockPage;

        if (StringUtils.hasText(name)) { 
            stockPage = itemRepository.findByNameContainingAndDeleted(name, false, pageable);
        } else {
            stockPage = itemRepository.findByDeleted(false, pageable); 
        }

        return stockPage.map(ItemResponse::from); 
    }
    

    public StockUserDetailResponse getEmployeeStockDetail(Long itemId) {
       
        Item item = itemRepository.findByItemIdAndDeletedFalse(itemId)
                .orElseThrow(() -> new InoutException("존재하지 않거나 삭제된 상품입니다.", 404, "ITEM_NOT_FOUND"));


        return StockUserDetailResponse.from(item);
    }

    @Transactional(readOnly = true)
    public List<StockHistoryResponse> getMyStoreHistory(Long userId) {

        List<StockReceivingHistory> receiving = receivingHistoryRepository.findAllByUser_Id(userId);
        
        List<StockUsageHistory> usage = usageHistoryRepository.findAllByUser_Id(userId);

        List<StockHistoryResponse> combined = new ArrayList<>();

        receiving.forEach(r -> combined.add(StockHistoryResponse.from(r)));
        usage.forEach(u -> combined.add(StockHistoryResponse.from(u)));

        return combined.stream()
                .sorted(Comparator.comparing(StockHistoryResponse::getDate).reversed())
                .collect(Collectors.toList());
    }
    

    @Transactional(readOnly = true)
    public List<StockHistoryResponse> getStoreHistory(Long storeId) {
        List<StockReceivingHistory> receiving = 
            receivingHistoryRepository.findAllByUser_Store_Id(storeId);
        
        List<StockUsageHistory> usage = 
            usageHistoryRepository.findAllByUser_Store_Id(storeId);

        List<StockHistoryResponse> combined = new ArrayList<>();
        
        receiving.forEach(r -> combined.add(StockHistoryResponse.from(r)));
        usage.forEach(u -> combined.add(StockHistoryResponse.from(u)));

        return combined.stream()
                .sorted(Comparator.comparing(StockHistoryResponse::getDate).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 최근 7일 판매 속도와 안전재고를 기반으로 스마트 발주 추천 목록을 반환합니다.
     * Gemini 호출 없이 휴리스틱으로 산출하여 장바구니 화면에서 즉시 사용할 수 있습니다.
     */
    @Transactional(readOnly = true)
    public List<AiStockSuggestionResponse> getAiStockSuggestions(int limit) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Map<Long, Long> salesMap = new HashMap<>();
        for (Object[] row : usageHistoryRepository.sumRecentSalesByItem(sevenDaysAgo)) {
            salesMap.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }

        return itemRepository.findAllByDeletedFalse().stream()
                .map(item -> {
                    long recentSales = salesMap.getOrDefault(item.getItemId(), 0L);
                    int stock = item.getCurrentStock() != null ? item.getCurrentStock() : 0;
                    int min = item.getMinStockLevel() != null ? item.getMinStockLevel() : 0;
                    double dailyVelocity = recentSales / 7.0;
                    int daysLeft = dailyVelocity > 0 ? (int) Math.floor(stock / dailyVelocity) : (stock > min ? 99 : 0);

                    boolean needsReorder = stock <= min || (dailyVelocity > 0 && daysLeft <= 3);
                    if (!needsReorder) {
                        return null;
                    }

                    int recommendQty = Math.max(min * 2 - stock, Math.max(min, (int) Math.ceil(dailyVelocity * 7)));
                    if (recommendQty <= 0) {
                        recommendQty = Math.max(min, 1);
                    }

                    String reason;
                    if (stock <= 0) {
                        reason = "품절 상태입니다. 즉시 보충이 필요합니다.";
                    } else if (stock <= min) {
                        reason = String.format("안전재고(%d) 미달 · 현재 %d", min, stock);
                    } else {
                        reason = String.format("최근 7일 판매 %d개 · 약 %d일 내 소진 예상", recentSales, daysLeft);
                    }

                    return AiStockSuggestionResponse.builder()
                            .itemId(item.getItemId())
                            .itemName(item.getName())
                            .currentStock(stock)
                            .minStockLevel(min)
                            .recommendQty(recommendQty)
                            .reason(reason)
                            .unitPrice(item.getUnitPrice())
                            .recentSalesQty(recentSales)
                            .build();
                })
                .filter(s -> s != null)
                .sorted(Comparator
                        .comparingInt((AiStockSuggestionResponse s) -> s.getCurrentStock())
                        .thenComparing(Comparator.comparingLong(AiStockSuggestionResponse::getRecentSalesQty).reversed()))
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }
}
