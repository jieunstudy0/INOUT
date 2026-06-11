package com.jstudy.inout.order.repository;

import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRequestRepository extends JpaRepository<OrderRequest, Long> {

    long countByStatus(OrderStatus status);

    @EntityGraph(attributePaths = {"orderDetails", "orderDetails.item", "requestUser", "requestUser.store"})
    List<OrderRequest> findAllByRequestUser_IdOrderByRequestDateDesc(Long userId);

    @EntityGraph(attributePaths = {"orderDetails", "orderDetails.item", "requestUser", "requestUser.store"})
    List<OrderRequest> findAllByStatusOrderByRequestDateDesc(OrderStatus status);


	@Query("SELECT DISTINCT o FROM OrderRequest o " +
	        "JOIN FETCH o.requestUser u " +
	        "JOIN FETCH u.store " +
	        "LEFT JOIN FETCH o.orderDetails d " +
	        "LEFT JOIN FETCH d.item " +
	        "ORDER BY o.requestDate DESC")
	 List<OrderRequest> findAllWithDetailsOrderByDateDesc();
	
	
	 @Query("SELECT DISTINCT o FROM OrderRequest o " +
	        "JOIN FETCH o.requestUser u " +
	        "JOIN FETCH u.store " +
	        "LEFT JOIN FETCH o.orderDetails d " +
	        "LEFT JOIN FETCH d.item " +
	        "WHERE o.status = :status " +
	        "ORDER BY o.requestDate DESC")
	 List<OrderRequest> findAllWithDetailsByStatusOrderByDateDesc(
	         @Param("status") OrderStatus status);

    @EntityGraph(attributePaths = {"requestUser", "requestUser.store", "orderDetails", "orderDetails.item"})
    @Query("select o from OrderRequest o where o.status = :status")
    List<OrderRequest> findAllWithDetailsByStatus(OrderStatus status);

    @EntityGraph(attributePaths = {"orderDetails", "orderDetails.item", "requestUser", "requestUser.store"})
    @Query("select o from OrderRequest o where o.id = :id")
    Optional<OrderRequest> findWithDetailsGraphById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderRequest o where o.id = :id")
    Optional<OrderRequest> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"orderDetails", "orderDetails.item", "requestUser", "requestUser.store"})
    @Query("select o from OrderRequest o where o.id = :id")
    Optional<OrderRequest> findByIdForUpdateWithDetails(@Param("id") Long id);

    @Query("SELECT COUNT(o) FROM OrderRequest o WHERE o.requestDate >= :startOfDay")
    long countTodayOrders(@Param("startOfDay") java.time.LocalDateTime startOfDay);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM OrderRequest o WHERE o.requestDate >= :startOfDay AND o.status IN :statuses")
    Long sumTodayOrderAmount(@Param("startOfDay") java.time.LocalDateTime startOfDay,
                             @Param("statuses") java.util.List<com.jstudy.inout.order.entity.OrderStatus> statuses);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"requestUser", "requestUser.store"})
    @Query("SELECT o FROM OrderRequest o ORDER BY o.requestDate DESC")
    java.util.List<OrderRequest> findRecentOrders(org.springframework.data.domain.Pageable pageable);
        
    @Query("SELECT o FROM OrderRequest o JOIN FETCH o.orderDetails WHERE o.id = :id")
    Optional<OrderRequest> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT YEAR(o.requestDate) as year, MONTH(o.requestDate) as month, SUM(o.totalPrice) as totalAmount " +
           "FROM OrderRequest o " +
           "WHERE o.status IN ('PAID', 'PARTIAL', 'COMPLETED') AND o.requestDate >= :startDate " +
           "GROUP BY YEAR(o.requestDate), MONTH(o.requestDate) " +
           "ORDER BY year ASC, month ASC")
    List<Object[]> findMonthlyOrderAmountTrend(@Param("startDate") LocalDateTime startDate);


    @Query("SELECT COALESCE(s.name, '미지정 매장') as storeName, COUNT(o) as orderCount " +
           "FROM OrderRequest o JOIN o.requestUser u LEFT JOIN u.store s " +
           "WHERE o.requestDate >= :startDate " +
           "GROUP BY s.name " +
           "ORDER BY orderCount DESC")
    List<Object[]> findTopStoreOrderFrequency(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    long countByRequestUser_Id(Long userId);
    
    long countByRequestUser_IdAndStatus(Long userId, OrderStatus status);
    
    long countByRequestUser_IdAndStatusIn(Long userId, List<OrderStatus> statuses);

    List<OrderRequest> findAllByRequestUser_IdAndStatusInOrderByRequestDateDesc(
            Long userId, List<OrderStatus> statuses, Pageable pageable);

}