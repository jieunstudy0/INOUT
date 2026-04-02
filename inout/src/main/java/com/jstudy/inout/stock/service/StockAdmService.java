package com.jstudy.inout.stock.service;

import java.util.List;
import org.springframework.data.domain.Pageable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.stock.dto.admin.StockAdmRequest;
import com.jstudy.inout.stock.dto.admin.StockAdminResponse;
import com.jstudy.inout.stock.dto.admin.StockDetailResponse;
import com.jstudy.inout.stock.dto.admin.StockRegister;
import com.jstudy.inout.stock.dto.admin.StockUpdate;
import com.jstudy.inout.stock.dto.emp.StockHistoryResponse;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.ItemCategory;
import com.jstudy.inout.stock.entity.StockReceivingHistory;
import com.jstudy.inout.stock.entity.StockUsageHistory;
import com.jstudy.inout.stock.repository.ItemCategoryRepository;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockReceivingHistoryRepository;
import com.jstudy.inout.stock.repository.StockUsageHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional(readOnly = true)
public class StockAdmService {

	private final ItemRepository itemRepository;
    private final ItemCategoryRepository itemCategoryRepository;
    private final StockReceivingHistoryRepository receivingHistoryRepository;
    private final UserRepository userRepository;
    private final StockUsageHistoryRepository usageHistoryRepository; 
 
    @Transactional 
    public Long registerStock(StockRegister stockRegister) {
    	
    	log.info("상품 등록 시도: name={}, categoryId={}", stockRegister.getName(), stockRegister.getCategoryId());

        ItemCategory category = itemCategoryRepository.findById(stockRegister.getCategoryId())
                .orElseThrow(() -> new InoutException("해당 카테고리를 찾을 수 없습니다.", 404, "CATEGORY_NOT_FOUND"));

        if (itemRepository.existsByName(stockRegister.getName())) {
            throw new InoutException("이미 등록된 상품명입니다.", 400, "DUPLICATE_ITEM_NAME");
        }

        Item item = stockRegister.toEntity(category);
        Item savedItem = itemRepository.save(item);
        
        log.info("상품 등록 성공: id={}, name={}", savedItem.getItemId(), savedItem.getName());

        return savedItem.getItemId(); 
    }
        
    @Transactional
    public void updateStock(Long itemId, StockUpdate stockUpdate, Long userId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new InoutException("상품을 찾을 수 없습니다.", 404, "ITEM_NOT_FOUND"));

        ItemCategory category = itemCategoryRepository.findById(stockUpdate.getCategoryId())
                .orElseThrow(() -> new InoutException("카테고리를 찾을 수 없습니다.", 404, "CATEGORY_NOT_FOUND"));

        item.updateInfo(
            stockUpdate.getName(), 
            category, 
            stockUpdate.getUnitPrice(), 
            stockUpdate.getMinStockLevel(), 
            stockUpdate.getUnitDescription(), 
            stockUpdate.getDescription()     
        );
    }

    @Transactional
    public void deleteStock(Long itemId) {

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new InoutException("삭제하려는 상품이 존재하지 않습니다.", 404, "ITEM_NOT_FOUND"));

        if (item.getDeleted()) {
            throw new InoutException("이미 삭제된 상품입니다.", 400, "ALREADY_DELETED");
        }
        
        item.delete(); 
    }

    @Transactional(readOnly = true)
    public Page<StockAdminResponse> getAdminStockList(String name, boolean deleted, Pageable pageable) {
        
    	Page<Item> stockPage;

        if (StringUtils.hasText(name)) {
            stockPage = itemRepository.findByNameContainingAndDeleted(name, deleted, pageable);
        } 

        else {
            stockPage = itemRepository.findByDeleted(deleted, pageable);
        }

        return stockPage.map(StockAdminResponse::from);
    }
    
    @Transactional
    public Long receiveStock(Long itemId, int quantity, Long userId, String memo) {

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new InoutException("상품을 찾을 수 없습니다.", 404, "ITEM_NOT_FOUND"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404, "USER_NOT_FOUND"));

        item.addStock(quantity); 

        StockReceivingHistory history = StockReceivingHistory.builder()
                .item(item)
                .user(user)
                .receivingQuantity(quantity)
                .resultStock(item.getCurrentStock()) 
                .memo(memo)
                .build();

        receivingHistoryRepository.save(history);

        return item.getItemId();
    }

    @Transactional(readOnly = true)
    public List<StockHistoryResponse> getUnifiedHistory(Long itemId, int page, int size) {
    	
        List<StockReceivingHistory> receiving = 
            receivingHistoryRepository.findAllByItem_ItemId(itemId);
        
        List<StockUsageHistory> usage = 
            usageHistoryRepository.findAllByItem_ItemId(itemId);

        List<StockHistoryResponse> combined = new ArrayList<>();
        receiving.forEach(r -> combined.add(StockHistoryResponse.from(r)));
        usage.forEach(u -> combined.add(StockHistoryResponse.from(u)));

        return combined.stream()
                .sorted(Comparator.comparing(StockHistoryResponse::getDate).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    private List<StockHistoryResponse> getAllHistoryForStats(Long itemId) {
        List<StockReceivingHistory> receiving = 
            receivingHistoryRepository.findAllByItem_ItemId(itemId);
        
        List<StockUsageHistory> usage = 
            usageHistoryRepository.findAllByItem_ItemId(itemId);

        List<StockHistoryResponse> combined = new ArrayList<>();
        receiving.forEach(r -> combined.add(StockHistoryResponse.from(r)));
        usage.forEach(u -> combined.add(StockHistoryResponse.from(u)));

        return combined;
    }

    public List<StockAdminResponse> getLowStockAlerts() {
        return itemRepository.findLowStockItems().stream()
                .map(StockAdminResponse::from)
                .collect(Collectors.toList());
    }

    public List<StockAdminResponse> getOutOfStockItems() {
        return itemRepository.findOutOfStockItems().stream()
                .map(StockAdminResponse::from)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public StockDetailResponse getStockDetail(Long itemId, int page, int size) {

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new InoutException("상품을 찾을 수 없습니다.", 404, "ITEM_NOT_FOUND"));

        List<StockHistoryResponse> pagedHistory = getUnifiedHistory(itemId, page, size);

        List<StockHistoryResponse> allHistory = getAllHistoryForStats(itemId);

        String status = "정상";
        if (item.getCurrentStock() == 0) status = "품절";
        else if (item.getCurrentStock() <= item.getMinStockLevel()) status = "저재고";

        return StockDetailResponse.builder()
                .itemId(item.getItemId())
                .itemName(item.getName())
                .categoryName(item.getCategory() != null ? item.getCategory().getCategoryName() : "미지정")
                .currentStock(item.getCurrentStock())
                .minStockLevel(item.getMinStockLevel())
                .status(status)
                .history(pagedHistory)      
                .totalReceived(allHistory.stream()   
                        .filter(h -> h.getType().equals("입고"))
                        .mapToLong(StockHistoryResponse::getQuantity)
                        .sum())
                .totalUsed(allHistory.stream()      
                        .filter(h -> h.getType().equals("사용"))
                        .mapToLong(h -> Math.abs(h.getQuantity()))
                        .sum())
                .build();
    }

    @Transactional
    public void adjustStock(Long adminId, StockAdmRequest request) {

        Item item = itemRepository.findByIdWithLock(request.getItemId())
                .orElseThrow(() -> new InoutException("상품 없음"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new InoutException("관리자 정보 없음"));

        int diff = request.getActualStock() - item.getCurrentStock();

        if (diff == 0) {
            return; 
        }

        if (diff > 0) {
            StockReceivingHistory receiving = StockReceivingHistory.builder()
                    .item(item)
                    .user(admin)
                    .receivingQuantity(diff)
                    .resultStock(request.getActualStock()) 
                    .memo("[재고조정] " + request.getReason()) 
                    .build();

            receivingHistoryRepository.save(receiving);
            item.addStock(diff); 

        } else {   
        	
            int usageQuantity = Math.abs(diff); 
            
            StockUsageHistory usage = StockUsageHistory.builder()
                    .item(item)
                    .user(admin)
                    .usageQuantity(usageQuantity) 
                    .resultStock(request.getActualStock()) 
                    .memo("[재고조정] " + request.getReason())
                    .build();

            usageHistoryRepository.save(usage);
            item.removeStock(usageQuantity); 
        }
    }
}
