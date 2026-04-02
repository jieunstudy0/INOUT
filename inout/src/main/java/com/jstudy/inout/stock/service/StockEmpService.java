package com.jstudy.inout.stock.service;

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
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

        List<StockReceivingHistory> receiving = receivingHistoryRepository.findAllByUser_UserId(userId);
        
        List<StockUsageHistory> usage = usageHistoryRepository.findAllByUser_UserId(userId);

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
            receivingHistoryRepository.findAllByUser_Store_StoreId(storeId);
        
        List<StockUsageHistory> usage = 
            usageHistoryRepository.findAllByUser_Store_StoreId(storeId);

        List<StockHistoryResponse> combined = new ArrayList<>();
        
        receiving.forEach(r -> combined.add(StockHistoryResponse.from(r)));
        usage.forEach(u -> combined.add(StockHistoryResponse.from(u)));

        return combined.stream()
                .sorted(Comparator.comparing(StockHistoryResponse::getDate).reversed())
                .collect(Collectors.toList());
    }
    
}
