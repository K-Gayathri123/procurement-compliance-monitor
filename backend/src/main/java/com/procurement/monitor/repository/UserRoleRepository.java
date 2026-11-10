package com.procurement.monitor.repository;

import com.procurement.monitor.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRoleRepository
    extends JpaRepository<UserRole, UserRoleId>,
            JpaSpecificationExecutor<UserRole> {

    java.util.List<UserRole> findByUserId(Long value);
    java.util.List<UserRole> findByRoleId(Long value);
}
