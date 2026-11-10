package com.procurement.monitor.repository;

import com.procurement.monitor.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PurchaseOrderLineRepository
    extends JpaRepository<PurchaseOrderLine, Long>,
            JpaSpecificationExecutor<PurchaseOrderLine> {

    java.util.List<PurchaseOrderLine> findByPoId(Long value);
}
