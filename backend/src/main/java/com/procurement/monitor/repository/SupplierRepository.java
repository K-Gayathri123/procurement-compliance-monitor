package com.procurement.monitor.repository;

import com.procurement.monitor.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SupplierRepository
    extends JpaRepository<Supplier, Long>,
            JpaSpecificationExecutor<Supplier> {

    java.util.List<Supplier> findByDeletedByUserId(Long value);
}
