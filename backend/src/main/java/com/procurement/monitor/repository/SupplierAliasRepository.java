package com.procurement.monitor.repository;

import com.procurement.monitor.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SupplierAliasRepository
    extends JpaRepository<SupplierAlias, Long>,
            JpaSpecificationExecutor<SupplierAlias> {

    java.util.List<SupplierAlias> findBySupplierId(Long value);
    java.util.List<SupplierAlias> findByDeletedByUserId(Long value);
    java.util.Optional<SupplierAlias> findByNormalizedAlias(String normalizedAlias);
}
