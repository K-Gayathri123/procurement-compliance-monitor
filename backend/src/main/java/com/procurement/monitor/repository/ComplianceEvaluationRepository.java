package com.procurement.monitor.repository;

import com.procurement.monitor.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ComplianceEvaluationRepository
    extends JpaRepository<ComplianceEvaluation, Long>,
            JpaSpecificationExecutor<ComplianceEvaluation> {

    java.util.List<ComplianceEvaluation> findByPoLineId(Long value);
    java.util.List<ComplianceEvaluation> findByContractId(Long value);
    java.util.List<ComplianceEvaluation> findByTierId(Long value);
    java.util.List<ComplianceEvaluation> findByRateId(Long value);
    java.util.Optional<ComplianceEvaluation> findFirstByPoLineIdOrderByEvaluationIdDesc(Long poLineId);
}
