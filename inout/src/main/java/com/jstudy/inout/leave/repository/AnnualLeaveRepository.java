package com.jstudy.inout.leave.repository;

import com.jstudy.inout.leave.entity.AnnualLeave;
import com.jstudy.inout.leave.entity.LeaveStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnualLeaveRepository extends JpaRepository<AnnualLeave, Long> {

    @Query(value = "SELECT a FROM AnnualLeave a JOIN FETCH a.user WHERE a.user.id = :userId",
           countQuery = "SELECT COUNT(a) FROM AnnualLeave a WHERE a.user.id = :userId")
    Page<AnnualLeave> findByUserIdWithUser(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "SELECT a FROM AnnualLeave a JOIN FETCH a.user u LEFT JOIN FETCH u.store WHERE u.store.id = :storeId",
           countQuery = "SELECT COUNT(a) FROM AnnualLeave a WHERE a.user.store.id = :storeId")
    Page<AnnualLeave> findByStoreIdWithUser(@Param("storeId") Long storeId, Pageable pageable);

    @Query(value = "SELECT a FROM AnnualLeave a JOIN FETCH a.user u LEFT JOIN FETCH u.store " +
                   "WHERE u.store.id = :storeId AND a.status = :status",
           countQuery = "SELECT COUNT(a) FROM AnnualLeave a WHERE a.user.store.id = :storeId AND a.status = :status")
    Page<AnnualLeave> findByStoreIdAndStatusWithUser(
            @Param("storeId") Long storeId,
            @Param("status") LeaveStatus status,
            Pageable pageable);

    @Query(value = "SELECT a FROM AnnualLeave a JOIN FETCH a.user u LEFT JOIN FETCH u.store LEFT JOIN FETCH a.processor WHERE a.id = :leaveId")
    Optional<AnnualLeave> findByIdWithUser(@Param("leaveId") Long leaveId);

    @Query(value = "SELECT a FROM AnnualLeave a JOIN FETCH a.user WHERE a.status = :status",
           countQuery = "SELECT COUNT(a) FROM AnnualLeave a WHERE a.status = :status")
    Page<AnnualLeave> findByStatusWithUser(@Param("status") LeaveStatus status, Pageable pageable);

    @Query(value = "SELECT a FROM AnnualLeave a JOIN FETCH a.user",
           countQuery = "SELECT COUNT(a) FROM AnnualLeave a")
    Page<AnnualLeave> findAllWithUser(Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM AnnualLeave a " +
           "WHERE a.user.id = :userId AND a.status <> :excludedStatus " +
           "AND a.startDate <= :endDate AND a.endDate >= :startDate")
    boolean existsOverlapping(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludedStatus") LeaveStatus excludedStatus);

    @Query("SELECT COUNT(a) FROM AnnualLeave a WHERE a.user.store.id = :storeId AND a.status = :status")
    long countByStoreIdAndStatus(@Param("storeId") Long storeId, @Param("status") LeaveStatus status);
}
