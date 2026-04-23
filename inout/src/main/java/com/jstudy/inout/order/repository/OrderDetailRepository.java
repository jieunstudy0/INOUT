package com.jstudy.inout.order.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.jstudy.inout.order.entity.OrderDetail;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    Optional<OrderDetail> findByOrderDetailIdAndOrderRequest_Id(Long orderDetailId, Long orderId);
}
