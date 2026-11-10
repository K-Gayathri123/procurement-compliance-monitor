package com.procurement.monitor.repository;

import com.procurement.monitor.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RoleRepository
    extends JpaRepository<Role, Long>,
            JpaSpecificationExecutor<Role> {

    java.util.Optional<Role> findByRoleName(String roleName);
}
