package com.jstudy.inout.stock.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.jstudy.inout.stock.entity.StoreStockChangeType;
import com.jstudy.inout.stock.entity.StoreStockHistory;

public interface StoreStockHistoryRepository extends JpaRepository<StoreStockHistory, Long> {

    @Query("SELECT h FROM StoreStockHistory h JOIN FETCH h.user " +
           "WHERE h.storeStock.id = :storeStockId ORDER BY h.id DESC")
    List<StoreStockHistory> findRecentByStoreStock(@Param("storeStockId") Long storeStockId, Pageable pageable);

    List<StoreStockHistory> findAllByStoreStock_Id(Long storeStockId);

    /** 기준 시각 이후 해당 종류의 기록이 있는 매장 재고 ID (실사표 '최근 사용' 대상) */
    @Query("SELECT DISTINCT h.storeStock.id FROM StoreStockHistory h " +
           "WHERE h.storeStock.store.id = :storeId AND h.type = :type AND h.createdAt >= :since")
    List<Long> findStoreStockIdsChangedSince(@Param("storeId") Long storeId,
                                             @Param("type") StoreStockChangeType type,
                                             @Param("since") LocalDateTime since);
}
