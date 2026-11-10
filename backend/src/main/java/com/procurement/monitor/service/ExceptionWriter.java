package com.procurement.monitor.service;

import com.procurement.monitor.entity.*;
import com.procurement.monitor.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExceptionWriter {

    private final ProcurementExceptionRepository exceptions;
    private final ReviewEventRepository events;

    public ExceptionWriter(
            ProcurementExceptionRepository exceptions,
            ReviewEventRepository events
    ) {
        this.exceptions = exceptions;
        this.events = events;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ProcurementException create(
            ComplianceEvaluation evaluation,
            String type,
            String groupKey,
            String reason,
            Long actorId,
            String actorRole
    ) {
        ProcurementException exception =
                new ProcurementException();

        exception.setEvaluationId(
                evaluation.getEvaluationId()
        );
        exception.setExceptionType(type);
        exception.setRelatedGroupKey(groupKey);
        exception.setReviewStatus("OPEN");
        exception.setReason(reason);

        exception = exceptions.saveAndFlush(exception);

        ReviewEvent event = new ReviewEvent();
        event.setExceptionId(
                exception.getExceptionId()
        );
        event.setActorUserId(actorId);
        event.setActorRoleAtAction(actorRole);
        event.setAction("EXCEPTION_CREATED");
        event.setComment(reason);

        events.save(event);

        return exception;
    }
}
