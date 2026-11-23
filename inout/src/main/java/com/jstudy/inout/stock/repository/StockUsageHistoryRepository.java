package com.jstudy.inout.stock.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jstudy.inout.stock.entity.StockUsageHistory;

public interface StockUsageHistoryRepository extends JpaRepository<StockUsageHistory, Long> {
	
	List<StockUsageHistory> findAllByItem_ItemId(Long itemId);

	List<StockUsageHistory> findAllByUser_Id(Long userId);

    int countByProcessDateAfter(LocalDateTime startOfDay);

	List<StockUsageHistory> findAllByUser_Store_Id(Long storeId);

    @Query("SELECT i.name as itemName, SUM(s.usageQuantity) as totalConsumed " +
           "FROM StockUsageHistory s JOIN s.item i " +
           "WHERE s.processDate >= :startDate " +
           "GROUP BY i.name " +
           "ORDER BY totalConsumed DESC")
    List<Object[]> findTopConsumedItems(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    @Query("SELECT i.itemId, COALESCE(SUM(s.usageQuantity), 0) " +
           "FROM StockUsageHistory s JOIN s.item i " +
           "WHERE s.processDate >= :startDate " +
           "GROUP BY i.itemId")
    List<Object[]> sumRecentSalesByItem(@Param("startDate") LocalDateTime startDate);

    /** 기준 시각 이후 사용(출고) 기록이 있는 삭제되지 않은 상품 ID. 실사표 대상 선정에 쓴다. */
    @Query("SELECT DISTINCT s.item.itemId FROM StockUsageHistory s " +
           "WHERE s.processDate >= :since AND s.item.deleted = false")
    List<Long> findItemIdsUsedSince(@Param("since") LocalDateTime since);
    
    
int countByUserIdAndProcessDateAfter(Long userId, LocalDateTime processDate);
    
    List<StockUsageHistory> findByUser_IdAndProcessDateAfterOrderByProcessDateDesc(
            Long userId, LocalDateTime processDate, Pageable pageable);
}
