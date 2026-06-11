package com.jstudy.inout.payment.repository;

import com.jstudy.inout.payment.entity.DepositHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepositHistoryRepository extends JpaRepository<DepositHistory, Long> {
    
    Page<DepositHistory> findByDepositAccountIdOrderByCreatedAtDesc(Long depositAccountId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM DepositHistory d " +
           "WHERE d.type = 'CHARGE' " +
           "AND YEAR(d.createdAt) = YEAR(CURRENT_DATE) " +
           "AND MONTH(d.createdAt) = MONTH(CURRENT_DATE)")
    long calculateMonthlyCharge();

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM DepositHistory d " +
           "WHERE d.type = 'USE' " +
           "AND YEAR(d.createdAt) = YEAR(CURRENT_DATE) " +
           "AND MONTH(d.createdAt) = MONTH(CURRENT_DATE)")
    long calculateMonthlyUsage();

    @Query("SELECT d FROM DepositHistory d WHERE " +
           "(:storeId IS NULL OR d.depositAccount.user.id = :storeId) AND " +
           "(:type IS NULL OR :type = '' OR CAST(d.type AS string) = :type) AND " +
           "(:keyword IS NULL OR :keyword = '' OR d.depositAccount.user.name LIKE CONCAT('%', :keyword, '%'))")
    Page<DepositHistory> findAdminHistoriesByFilters(
            @Param("storeId") Long storeId,
            @Param("type") String type,
            @Param("keyword") String keyword,
            Pageable pageable);
}