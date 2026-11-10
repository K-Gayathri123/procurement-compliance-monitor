package com.procurement.monitor.repository;

import com.procurement.monitor.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReviewEventRepository
    extends JpaRepository<ReviewEvent, Long>,
            JpaSpecificationExecutor<ReviewEvent> {

    java.util.List<ReviewEvent> findByExceptionId(Long value);
    java.util.List<ReviewEvent> findByActorUserId(Long value);
}
