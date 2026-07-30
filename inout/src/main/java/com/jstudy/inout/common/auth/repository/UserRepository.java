package com.jstudy.inout.common.auth.repository;

import java.util.Optional;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{

	@EntityGraph(attributePaths = {"userRoles", "userRoles.role", "store"}) 
	Optional<User> findByEmail(String email);
	boolean existsByEmail(String email);
	Optional<User>findByNameAndPhone(String name, String phone);
	Optional<User>findByEmailAndNameAndPhone(String email, String name, String phone);
	Optional<User> findByPasswordResetKey(String resetKey);
	Optional<User> findByEmailAndPassword(String email, String password);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deleted = false")
    Optional<User> findByEmailActive(@Param("email") String email); // 💡 @Param 추가
    
    long countByStatus(UserStatus status);
    long countByIsLockedTrue();
    long countByStore_Id(Long storeId);
    long countByStore_IdAndIsLockedTrue(Long storeId);
    
    
    @Query("SELECT u FROM User u WHERE " +
            "(:storeId IS NULL OR (:storeId = 0 AND u.store IS NULL) OR u.store.id = :storeId) AND " +
            "(:status IS NULL OR u.status = :status) AND " +
            "(:keyword IS NULL OR :keyword = '' " +
            "OR u.name LIKE CONCAT('%', :keyword, '%') " +
            "OR u.email LIKE CONCAT('%', :keyword, '%') " +
          
            "OR REPLACE(u.phone, '-', '') LIKE CONCAT('%', REPLACE(:keyword, '-', ''), '%'))")
     Page<User> findAdminUsersByFilters(
             @Param("storeId") Long storeId,
             @Param("status") UserStatus status,
             @Param("keyword") String keyword,
             Pageable pageable);

    @Query("""
            SELECT u
            FROM User u
            JOIN u.userRoles ur
            JOIN ur.role r
            WHERE r.roleName = 'ROLE_ADMIN'
            ORDER BY u.id ASC
            """)
    java.util.List<User> findAdminUsersSortedById(Pageable pageable);

    @Query("""
            SELECT u
            FROM User u
            WHERE u.store.id = :storeId
              AND (:status IS NULL OR u.status = :status)
              AND (:keyword IS NULL OR :keyword = ''
                   OR u.name LIKE CONCAT('%', :keyword, '%')
                   OR u.email LIKE CONCAT('%', :keyword, '%')
                   OR REPLACE(u.phone, '-', '') LIKE CONCAT('%', REPLACE(:keyword, '-', ''), '%'))
            """)
    Page<User> findOwnerUsersByFilters(
            @Param("storeId") Long storeId,
            @Param("status") UserStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);
}
