package com.jstudy.inout.delivery.repository;

import com.jstudy.inout.delivery.entity.Delivery;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByOrderRequest_Id(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Delivery d where d.orderRequest.id = :orderId")
    Optional<Delivery> findByOrderIdForUpdate(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Delivery d join fetch d.orderRequest where d.id = :deliveryId")
    Optional<Delivery> findByIdForUpdate(@Param("deliveryId") Long deliveryId);
    
    List<Delivery> findByStatusAndShippedAtBefore(DeliveryStatus status, LocalDateTime shippedAt);

    long countByStatus(DeliveryStatus status);

    @Query(value = "SELECT d FROM Delivery d JOIN FETCH d.orderRequest WHERE d.status = :status",
           countQuery = "SELECT COUNT(d) FROM Delivery d WHERE d.status = :status")
    Page<Delivery> findByStatusWithOrder(@Param("status") DeliveryStatus status, Pageable pageable);

    @Query(value = "SELECT d FROM Delivery d JOIN FETCH d.orderRequest",
           countQuery = "SELECT COUNT(d) FROM Delivery d")
    Page<Delivery> findAllWithOrder(Pageable pageable);

    @Query(
            value = """
                    SELECT d
                    FROM Delivery d
                    JOIN FETCH d.orderRequest o
                    JOIN FETCH o.requestUser u
                    WHERE u.id = :userId
                    """,
            countQuery = """
                    SELECT COUNT(d)
                    FROM Delivery d
                    JOIN d.orderRequest o
                    JOIN o.requestUser u
                    WHERE u.id = :userId
                    """)
    Page<Delivery> findByUserIdWithOrder(@Param("userId") Long userId, Pageable pageable);

    @Query(
            value = """
                    SELECT d
                    FROM Delivery d
                    JOIN FETCH d.orderRequest o
                    JOIN FETCH o.requestUser u
                    JOIN FETCH u.store s
                    WHERE s.id = :storeId
                    """,
            countQuery = """
                    SELECT COUNT(d)
                    FROM Delivery d
                    JOIN d.orderRequest o
                    JOIN o.requestUser u
                    JOIN u.store s
                    WHERE s.id = :storeId
                    """)
    Page<Delivery> findByStoreIdWithOrder(@Param("storeId") Long storeId, Pageable pageable);

    @Query(
            value = """
                    SELECT d
                    FROM Delivery d
                    JOIN FETCH d.orderRequest o
                    JOIN FETCH o.requestUser u
                    WHERE u.id = :userId AND d.status = :status
                    """,
            countQuery = """
                    SELECT COUNT(d)
                    FROM Delivery d
                    JOIN d.orderRequest o
                    JOIN o.requestUser u
                    WHERE u.id = :userId AND d.status = :status
                    """)
    Page<Delivery> findByUserIdAndStatusWithOrder(
            @Param("userId") Long userId,
            @Param("status") DeliveryStatus status,
            Pageable pageable);

    @Query(
            value = """
                    SELECT d
                    FROM Delivery d
                    JOIN FETCH d.orderRequest o
                    JOIN FETCH o.requestUser u
                    JOIN FETCH u.store s
                    WHERE s.id = :storeId AND d.status = :status
                    """,
            countQuery = """
                    SELECT COUNT(d)
                    FROM Delivery d
                    JOIN d.orderRequest o
                    JOIN o.requestUser u
                    JOIN u.store s
                    WHERE s.id = :storeId AND d.status = :status
                    """)
    Page<Delivery> findByStoreIdAndStatusWithOrder(
            @Param("storeId") Long storeId,
            @Param("status") DeliveryStatus status,
            Pageable pageable);

    @Query("""
            SELECT COUNT(d)
            FROM Delivery d
            JOIN d.orderRequest o
            JOIN o.requestUser u
            JOIN u.store s
            WHERE s.id = :storeId AND d.status = :status
            """)
    long countByStoreIdAndStatus(@Param("storeId") Long storeId, @Param("status") DeliveryStatus status);
}
