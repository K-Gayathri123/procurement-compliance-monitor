package com.procurement.monitor.repository;

import com.procurement.monitor.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UploadBatchRepository
    extends JpaRepository<UploadBatch, Long>,
            JpaSpecificationExecutor<UploadBatch> {

    java.util.List<UploadBatch> findByUploadedByUserId(Long value);
    boolean existsByFileHash(String fileHash);
}
