package com.jstudy.inout.order.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.jstudy.inout.order.entity.Cart;
import com.jstudy.inout.order.entity.CartDetail;
import com.jstudy.inout.stock.entity.Item;

public interface CartDetailRepository extends JpaRepository<CartDetail, Long> {  
 
    List<CartDetail> findAllByCart_User_UserId(Long userId);

    @Query("SELECT cd FROM CartDetail cd " +
           "JOIN FETCH cd.cart c " +
           "JOIN FETCH c.user u " +
           "WHERE cd.cartDetailId IN :ids")
    List<CartDetail> findWithCartAndUserByIds(@Param("ids") List<Long> ids);
    
    Optional<CartDetail> findByCartAndItem(Cart cart, Item item);

    @Modifying
    @Query("UPDATE CartDetail cd SET cd.deleted = true WHERE cd.cartDetailId IN :ids")
    void updateDeletedStatusInBatch(@Param("ids") List<Long> ids);

    @Modifying
    @Query("UPDATE CartDetail cd SET cd.deleted = true " +
           "WHERE cd.cart.user.userId = :userId AND cd.deleted = false")
    void updateAllDeletedStatusByUserId(@Param("userId") Long userId);
}
