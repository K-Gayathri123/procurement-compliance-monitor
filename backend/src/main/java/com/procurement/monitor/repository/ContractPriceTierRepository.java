package com.procurement.monitor.repository;

import com.procurement.monitor.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ContractPriceTierRepository
    extends JpaRepository<ContractPriceTier, Long>,
            JpaSpecificationExecutor<ContractPriceTier> {

    java.util.List<ContractPriceTier> findByContractId(Long value);
}
