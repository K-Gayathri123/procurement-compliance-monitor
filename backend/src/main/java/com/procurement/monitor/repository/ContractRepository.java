package com.procurement.monitor.repository;

import com.procurement.monitor.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ContractRepository
    extends JpaRepository<Contract, Long>,
            JpaSpecificationExecutor<Contract> {

    java.util.List<Contract> findBySupplierId(Long value);
}
