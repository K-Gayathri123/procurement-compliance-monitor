package com.procurement.monitor.repository;

import com.procurement.monitor.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecoveryRecordRepository
    extends JpaRepository<RecoveryRecord, Long>,
            JpaSpecificationExecutor<RecoveryRecord> {

    java.util.List<RecoveryRecord> findByExceptionId(Long value);
    java.util.List<RecoveryRecord> findByRecordedByUserId(Long value);
}
