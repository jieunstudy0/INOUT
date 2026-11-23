package com.jstudy.inout.stock.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.jstudy.inout.stock.entity.StoreStock;
import jakarta.persistence.LockModeType;

public interface StoreStockRepository extends JpaRepository<StoreStock, Long> {

    @Query("SELECT s FROM StoreStock s JOIN FETCH s.item i " +
           "WHERE s.store.id = :storeId AND i.deleted = false ORDER BY i.name")
    List<StoreStock> findAllByStoreWithItem(@Param("storeId") Long storeId);

    @Query("SELECT s FROM StoreStock s JOIN FETCH s.item i " +
           "WHERE s.store.id = :storeId AND i.deleted = false AND s.quantity <= s.minStockLevel")
    List<StoreStock> findLowStockByStore(@Param("storeId") Long storeId);

    @Query("SELECT COUNT(s) FROM StoreStock s " +
           "WHERE s.store.id = :storeId AND s.item.deleted = false AND s.quantity <= s.minStockLevel")
    long countLowStockByStore(@Param("storeId") Long storeId);

    @Query("SELECT COUNT(s) FROM StoreStock s " +
           "WHERE s.store.id = :storeId AND s.item.deleted = false AND s.quantity = 0")
    long countOutOfStockByStore(@Param("storeId") Long storeId);

    Optional<StoreStock> findByStore_IdAndItem_ItemId(Long storeId, Long itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StoreStock s WHERE s.store.id = :storeId AND s.item.itemId = :itemId")
    Optional<StoreStock> findByStoreAndItemWithLock(@Param("storeId") Long storeId, @Param("itemId") Long itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StoreStock s WHERE s.id = :id")
    Optional<StoreStock> findByIdWithLock(@Param("id") Long id);

    /** 재고가 있는 매장 ID (실사표 자동 생성 대상) */
    @Query("SELECT DISTINCT s.store.id FROM StoreStock s")
    List<Long> findStoreIdsWithStock();
}
