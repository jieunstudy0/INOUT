package com.jstudy.inout.request.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.jstudy.inout.request.entity.ChangeRequest;
import com.jstudy.inout.request.entity.ChangeRequestCategory;
import com.jstudy.inout.request.entity.ChangeRequestStatus;
import jakarta.persistence.LockModeType;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {

    @Query(value = "SELECT r FROM ChangeRequest r JOIN FETCH r.requester WHERE r.requester.id = :userId ORDER BY r.id DESC",
           countQuery = "SELECT COUNT(r) FROM ChangeRequest r WHERE r.requester.id = :userId")
    Page<ChangeRequest> findMine(@Param("userId") Long userId, Pageable pageable);

    /** 점주 받은 요청함: 소속 매장의 점주 처리 요청 */
    @Query(value = "SELECT r FROM ChangeRequest r JOIN FETCH r.requester " +
                   "WHERE r.store.id = :storeId AND r.handler = :handler AND (:status IS NULL OR r.status = :status) ORDER BY r.id DESC",
           countQuery = "SELECT COUNT(r) FROM ChangeRequest r " +
                        "WHERE r.store.id = :storeId AND r.handler = :handler AND (:status IS NULL OR r.status = :status)")
    Page<ChangeRequest> findStoreInbox(@Param("storeId") Long storeId,
                                       @Param("handler") ChangeRequestCategory.Handler handler,
                                       @Param("status") ChangeRequestStatus status,
                                       Pageable pageable);

    /** 본사 받은 요청함: 모든 매장의 본사 처리 요청 */
    @Query(value = "SELECT r FROM ChangeRequest r JOIN FETCH r.requester JOIN FETCH r.store " +
                   "WHERE r.handler = :handler AND (:status IS NULL OR r.status = :status) ORDER BY r.id DESC",
           countQuery = "SELECT COUNT(r) FROM ChangeRequest r WHERE r.handler = :handler AND (:status IS NULL OR r.status = :status)")
    Page<ChangeRequest> findHqInbox(@Param("handler") ChangeRequestCategory.Handler handler,
                                    @Param("status") ChangeRequestStatus status,
                                    Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ChangeRequest r WHERE r.id = :id")
    Optional<ChangeRequest> findByIdWithLock(@Param("id") Long id);
}
