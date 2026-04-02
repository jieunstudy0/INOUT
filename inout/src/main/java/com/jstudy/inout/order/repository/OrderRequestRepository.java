package com.jstudy.inout.order.repository;

import com.jstudy.inout.order.entity.OrderRequest; 
import com.jstudy.inout.order.entity.OrderStatus;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRequestRepository extends JpaRepository<OrderRequest, Long> {

    long countByStatus(OrderStatus status);
    
    @EntityGraph(attributePaths = {"orderDetails", "orderDetails.item", "requestUser", "requestUser.store"})
    List<OrderRequest> findAllByRequestUser_UserIdOrderByRequestDateDesc(Long userId);
    
    List<OrderRequest> findAllByStatusOrderByRequestDateDesc(OrderStatus status);

    List<OrderRequest> findAllByOrderByRequestDateDesc();

    @EntityGraph(attributePaths = {"requestUser", "requestUser.store", "orderDetails"})
    @Query("select o from OrderRequest o where o.status = :status")
    List<OrderRequest> findAllWithDetailsByStatus(OrderStatus status);
}