package com.jstudy.inout.payment.repository;

import com.jstudy.inout.payment.entity.ChargeRequest;
import com.jstudy.inout.payment.entity.ChargeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChargeRequestRepository extends JpaRepository<ChargeRequest, Long> {
	
    List<ChargeRequest> findAllByRequestUser_IdOrderByRequestDateDesc(Long userId);

    List<ChargeRequest> findAllByStatusOrderByRequestDateDesc(ChargeStatus status);
}