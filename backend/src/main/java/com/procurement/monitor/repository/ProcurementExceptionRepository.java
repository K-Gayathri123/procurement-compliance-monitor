package com.procurement.monitor.repository;

import com.procurement.monitor.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProcurementExceptionRepository
    extends JpaRepository<ProcurementException, Long>,
            JpaSpecificationExecutor<ProcurementException> {

    java.util.List<ProcurementException> findByEvaluationId(Long value);
    java.util.List<ProcurementException> findByRelatedGroupKey(String relatedGroupKey);
}
