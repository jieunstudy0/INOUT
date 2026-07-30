package com.jstudy.inout.order.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.jstudy.inout.order.entity.OrderDetail;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {

    @EntityGraph(attributePaths = {"item", "orderRequest"})
    @Query("select od from OrderDetail od where od.orderDetailId = :detailId and od.orderRequest.id = :orderId")
    Optional<OrderDetail> findByOrderDetailIdAndOrderRequest_Id(
            @Param("detailId") Long orderDetailId,
            @Param("orderId") Long orderId);

    long countByIsAiSuggestedTrueAndStatus(com.jstudy.inout.order.entity.OrderDetailStatus status);
}
