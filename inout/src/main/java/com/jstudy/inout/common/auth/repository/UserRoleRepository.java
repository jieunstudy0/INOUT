package com.jstudy.inout.common.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jstudy.inout.common.auth.entity.Role;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.entity.UserRole;
import com.jstudy.inout.common.auth.entity.UserRoleId;
import com.jstudy.inout.common.auth.entity.UserStatus;


@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

	void deleteByUserAndRole(User user, Role adminRole);

    void deleteByUser(User user);

    /** 이 권한을 가진 사람 중 지금 로그인할 수 있는(재직 중·삭제 안 됨) 사람 수 */
    @Query("select count(ur) from UserRole ur where ur.role.roleName = :roleName"
            + " and ur.user.deleted = false and ur.user.status = :status")
    long countByRoleNameAndUserStatus(@Param("roleName") String roleName, @Param("status") UserStatus status);
}
