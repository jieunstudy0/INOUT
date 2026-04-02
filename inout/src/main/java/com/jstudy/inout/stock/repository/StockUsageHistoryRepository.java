package com.jstudy.inout.stock.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.jstudy.inout.stock.entity.StockUsageHistory;

public interface StockUsageHistoryRepository extends JpaRepository<StockUsageHistory, Long> {
	
	List<StockUsageHistory> findAllByItem_ItemId(Long itemId);

	List<StockUsageHistory> findAllByUser_UserId(Long userId);

    int countByProcessDateAfter(LocalDateTime startOfDay);

	List<StockUsageHistory> findAllByUser_Store_StoreId(Long storeId);
	
}
