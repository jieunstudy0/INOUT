package com.jstudy.inout.payment.repository;

import com.jstudy.inout.payment.entity.DepositAccount;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepositAccountRepository extends JpaRepository<DepositAccount, Long> {

	Optional<DepositAccount> findByUserId(Long userId);
	Optional<DepositAccount> findByStoreId(Long storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from DepositAccount a join fetch a.user u where u.id = :userId")
    Optional<DepositAccount> findByUserIdForUpdate(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from DepositAccount a join fetch a.store s where s.id = :storeId")
    Optional<DepositAccount> findByStoreIdForUpdate(@Param("storeId") Long storeId);
    
    @Query("SELECT COALESCE(SUM(da.balance), 0) FROM DepositAccount da")
    long calculateTotalDepositBalance();
 
    @Query("SELECT da FROM DepositAccount da JOIN FETCH da.user u LEFT JOIN FETCH u.store")
    List<DepositAccount> findAllWithUserAndStore();
}