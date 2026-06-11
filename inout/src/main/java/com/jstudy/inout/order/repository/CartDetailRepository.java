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
 
    List<CartDetail> findAllByCart_User_Id(Long userId);

    @Query("SELECT cd FROM CartDetail cd " +
           "JOIN FETCH cd.cart c " +
           "JOIN FETCH c.user u " +
           "JOIN FETCH cd.item " +
           "WHERE cd.cartDetailId IN :ids")
    List<CartDetail> findWithCartAndUserByIds(@Param("ids") List<Long> ids);

    @Query("SELECT cd FROM CartDetail cd " +
           "JOIN FETCH cd.item " +
           "JOIN FETCH cd.cart c " +
           "JOIN FETCH c.user u " +
           "WHERE u.id = :userId AND cd.deleted = false")
    List<CartDetail> findWithItemByCartUserIdNotDeleted(@Param("userId") Long userId);
    
    Optional<CartDetail> findByCartAndItem(Cart cart, Item item);

    @Modifying
    @Query("UPDATE CartDetail cd SET cd.deleted = true WHERE cd.cartDetailId IN :ids")
    void updateDeletedStatusInBatch(@Param("ids") List<Long> ids);

    @Modifying
    @Query("UPDATE CartDetail cd SET cd.deleted = true " +
           "WHERE cd.cart.user.id = :userId AND cd.deleted = false")
    void updateAllDeletedStatusByUserId(@Param("userId") Long userId);
    
    
    @Query("SELECT COUNT(c) FROM CartDetail c WHERE c.cart.user.id = :userId AND c.deleted = false")
    int countCartItemsByUserId(@Param("userId") Long userId);
}
