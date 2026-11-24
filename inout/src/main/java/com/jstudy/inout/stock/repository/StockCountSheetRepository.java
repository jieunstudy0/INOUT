package com.jstudy.inout.stock.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.jstudy.inout.stock.entity.StockCountSheet;
import com.jstudy.inout.stock.entity.StockCountStatus;
import jakarta.persistence.LockModeType;

public interface StockCountSheetRepository extends JpaRepository<StockCountSheet, Long> {

    boolean existsBySheetCode(String sheetCode);

    List<StockCountSheet> findAllByStore_IdOrderByIdDesc(Long storeId, Pageable pageable);

    List<StockCountSheet> findAllByStore_IdAndStatusOrderByIdDesc(Long storeId, StockCountStatus status, Pageable pageable);

    /** 승인·반려처럼 상태를 바꾸는 처리는 같은 실사표를 두 번 반영하지 않도록 행 락을 잡는다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StockCountSheet s where s.id = :id")
    Optional<StockCountSheet> findByIdWithLock(@Param("id") Long id);
}
