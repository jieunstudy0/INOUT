package com.jstudy.inout.stock.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.jstudy.inout.stock.entity.StockReceivingHistory;

public interface StockReceivingHistoryRepository extends JpaRepository<StockReceivingHistory, Long> {

	List<StockReceivingHistory> findAllByItem_ItemId(Long itemId);

	List<StockReceivingHistory> findAllByUser_Id(Long userId);

    int countByProcessDateAfter(LocalDateTime startOfDay);

	List<StockReceivingHistory> findAllByUser_Store_Id(Long storeId);
	
}
