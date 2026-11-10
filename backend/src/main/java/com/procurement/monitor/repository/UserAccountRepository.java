package com.procurement.monitor.repository;

import com.procurement.monitor.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserAccountRepository
    extends JpaRepository<UserAccount, Long>,
            JpaSpecificationExecutor<UserAccount> {

    java.util.List<UserAccount> findByDeletedByUserId(Long value);
    java.util.Optional<UserAccount> findByEmail(String email);
    boolean existsByEmail(String email);
}
