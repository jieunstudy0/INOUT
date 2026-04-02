package com.jstudy.inout.stock.repository;

import com.jstudy.inout.stock.entity.ItemCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemCategoryRepository extends JpaRepository<ItemCategory, Integer> {
		
	Optional<ItemCategory> findById(Long id);
	
}