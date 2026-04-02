package com.jstudy.inout.common.auth.repository;

import java.util.Optional;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.jstudy.inout.common.auth.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{

	Optional<User>findByEmail(String email);
	boolean existsByEmail(String email);
	Optional<User>findByNameAndPhone(String name, String phone);
	Optional<User>findByEmailAndNameAndPhone(String email, String name, String phone);
	Optional<User> findByPasswordResetKey(String resetKey);
	Optional<User> findByEmailAndPassword(String email, String password);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deleted = false")
    Optional<User> findByEmailActive(@Param("email") String email); // 💡 @Param 추가
}
