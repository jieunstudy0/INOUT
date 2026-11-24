package com.jstudy.inout.payment.repository;

import com.jstudy.inout.payment.entity.ChargeRequest;
import com.jstudy.inout.payment.entity.ChargeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ChargeRequestRepository extends JpaRepository<ChargeRequest, Long> {
	
    List<ChargeRequest> findAllByRequestUser_IdOrderByRequestDateDesc(Long userId);

    List<ChargeRequest> findAllByStatusOrderByRequestDateDesc(ChargeStatus status);

    /** 승인·반려 전에 행을 잠근다 — 같은 요청을 동시에 두 번 승인해 두 번 충전되지 않게 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ChargeRequest c where c.id = :id")
    Optional<ChargeRequest> findByIdForUpdate(@Param("id") Long id);
}