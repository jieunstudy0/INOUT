package com.jstudy.inout.order.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.jstudy.inout.order.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Long>{

	Optional<Cart> findByUser_UserId(Long userId);
	
}
