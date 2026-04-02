package com.jstudy.inout.common.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.jstudy.inout.common.auth.entity.UserRole;
import com.jstudy.inout.common.auth.entity.UserRoleId;


@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
}
