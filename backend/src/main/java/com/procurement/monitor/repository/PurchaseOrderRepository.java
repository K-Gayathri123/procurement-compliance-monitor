package com.procurement.monitor.repository;

import com.procurement.monitor.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PurchaseOrderRepository
    extends JpaRepository<PurchaseOrder, Long>,
            JpaSpecificationExecutor<PurchaseOrder> {

    java.util.List<PurchaseOrder> findByUploadId(Long value);
    java.util.List<PurchaseOrder> findBySupplierId(Long value);
    java.util.List<PurchaseOrder> findByBuyerUserId(Long value);
    java.util.Optional<PurchaseOrder> findBySourcePoNumber(String sourcePoNumber);
    boolean existsBySourcePoNumber(String sourcePoNumber);
}
