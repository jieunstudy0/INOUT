package com.jstudy.inout.stock.repository;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.jstudy.inout.stock.entity.Item;
import jakarta.persistence.LockModeType;

public interface ItemRepository extends JpaRepository<Item, Long> {
	
    boolean existsByName(String name); 

    Optional<Item> findByItemIdAndDeletedFalse(Long itemId);
    
    List<Item> findAllByDeletedFalse();

    boolean existsByNameAndDeletedFalse(String name);

    Page<Item> findByNameContainingAndDeleted(String name, boolean deleted, Pageable pageable);

    Page<Item> findByDeleted(boolean deleted, Pageable pageable);

    @Query("SELECT i FROM Item i WHERE i.deleted = false AND i.currentStock <= i.minStockLevel")
    List<Item> findLowStockItems();

    @Query("SELECT i FROM Item i WHERE i.deleted = false AND i.currentStock = 0")
    List<Item> findOutOfStockItems();

    @Query("SELECT count(i) FROM Item i WHERE i.deleted = false AND i.currentStock <= i.minStockLevel")
    long countLowStockItems();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.itemId = :id")
    Optional<Item> findByIdWithLock(@Param("id") Long id);
    
}
