package com.procurement.monitor.service;

import com.procurement.monitor.dto.request.*;
import com.procurement.monitor.dto.response.*;
import com.procurement.monitor.entity.*;
import com.procurement.monitor.mapper.ResponseMapper;
import com.procurement.monitor.repository.*;
import com.procurement.monitor.security.OperationalAccess;
import com.procurement.monitor.validation.Checks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ReviewWorkflowService {

    private static final Logger log =
        LoggerFactory.getLogger(ReviewWorkflowService.class);

    private static final Set<String> TERMINAL =
        Set.of(
            "APPROVED_EXCEPTION",
            "CONFIRMED_VIOLATION"
        );

    private final ProcurementExceptionRepository exceptions;
    private final ReviewEventRepository events;
    private final RecoveryRecordRepository recoveries;
    private final ComplianceEvaluationRepository evaluations;
    private final OperationalAccess access;

    public ReviewWorkflowService(
        ProcurementExceptionRepository exceptions,
        ReviewEventRepository events,
        RecoveryRecordRepository recoveries,
        ComplianceEvaluationRepository evaluations,
        OperationalAccess access
    ) {
        this.exceptions = exceptions;
        this.events = events;
        this.recoveries = recoveries;
        this.evaluations = evaluations;
        this.access = access;
    }

    @Transactional
    public ProcurementExceptionResponse explain(
        Long id,
        ExplanationRequest request,
        Long actorId
    ) {
        ProcurementException exception = get(id);
        PurchaseOrder order =
            access.orderFor(exception);

        boolean admin =
            access.has(actorId, "ADMIN");

        Checks.forbidden(
            !admin
                && (
                    !access.has(actorId, "BUYER")
                    || !Objects.equals(
                        order.getBuyerUserId(),
                        actorId
                    )
                ),
            "Only the purchase buyer or an admin may explain"
        );

        Checks.conflict(
            !Set.of(
                "OPEN", "EVIDENCE_REQUESTED"
            ).contains(
                exception.getReviewStatus()
            ),
            "Explanation is not allowed in the current status"
        );

        exception.setReviewStatus(
            "EXPLANATION_SUBMITTED"
        );

        recordEvent(
            exception,
            actorId,
            admin ? "ADMIN" : "BUYER",
            "SUBMIT_EXPLANATION",
            request.comment().trim()
        );

        exceptions.saveAndFlush(exception);

        return ResponseMapper.toResponse(
            exception
        );
    }

    @Transactional
    public ProcurementExceptionResponse decide(
        Long id,
        ReviewDecisionRequest request,
        Long actorId
    ) {
        String actorRole =
            access.reviewerRole(actorId);

        ProcurementException exception = get(id);
        PurchaseOrder order =
            access.orderFor(exception);

        Checks.conflict(
            TERMINAL.contains(
                exception.getReviewStatus()
            ),
            "Exception already has a terminal decision"
        );

        Checks.forbidden(
            Objects.equals(
                order.getBuyerUserId(),
                actorId
            ),
            "A reviewer cannot decide on their own purchase"
        );

        boolean explainedByActor =
            events.findByExceptionId(id)
                .stream()
                .anyMatch(event ->
                    actorId.equals(
                        event.getActorUserId()
                    )
                    && "SUBMIT_EXPLANATION".equals(
                        event.getAction()
                    )
                );

        Checks.forbidden(
            explainedByActor,
            "A person who submitted an explanation "
                + "cannot review this exception"
        );

        String nextStatus;

        switch (request.action()) {
            case "REQUEST_EVIDENCE" -> {
                Checks.conflict(
                    !Set.of(
                        "OPEN",
                        "EXPLANATION_SUBMITTED"
                    ).contains(
                        exception.getReviewStatus()
                    ),
                    "Evidence has already been requested"
                );

                nextStatus = "EVIDENCE_REQUESTED";
            }

            case "APPROVE_EXCEPTION" -> {
                requireExplanation(exception);
                nextStatus = "APPROVED_EXCEPTION";
            }

            case "CONFIRM_VIOLATION" -> {
                requireExplanation(exception);
                nextStatus = "CONFIRMED_VIOLATION";
            }

            default -> throw new IllegalArgumentException(
                "Unsupported reviewer action"
            );
        }

        exception.setReviewStatus(nextStatus);

        recordEvent(
            exception,
            actorId,
            actorRole,
            request.action(),
            request.comment().trim()
        );

        exceptions.saveAndFlush(exception);

        log.info(
            "Exception reviewed; exceptionId={}, action={}, actorId={}",
            id, request.action(), actorId
        );

        return ResponseMapper.toResponse(
            exception
        );
    }

    @Transactional(readOnly = true)
    public List<ReviewEventResponse> history(
        Long id, Long actorId
    ) {
        ProcurementException exception = get(id);

        access.readException(
            exception, actorId
        );

        return events.findByExceptionId(id)
            .stream()
            .sorted(
                Comparator.comparing(
                    ReviewEvent::getReviewEventId
                )
            )
            .map(ResponseMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<RecoveryRecordResponse> recoveries(
        Long id, Long actorId
    ) {
        ProcurementException exception = get(id);

        access.readException(
            exception, actorId
        );

        return recoveries.findByExceptionId(id)
            .stream()
            .sorted(
                Comparator.comparing(
                    RecoveryRecord::getRecoveryId
                )
            )
            .map(ResponseMapper::toResponse)
            .toList();
    }

    @Transactional
    public RecoveryRecordResponse recordRecovery(
        Long id,
        RecoveryRequest request,
        Long actorId
    ) {
        access.importRole(actorId);

        ProcurementException exception = get(id);

        Checks.conflict(
            !"CONFIRMED_VIOLATION".equals(
                exception.getReviewStatus()
            ),
            "Recovery records require a confirmed violation"
        );

        Checks.require(
            "PRICE_ABOVE_BAND".equals(
                exception.getExceptionType()
            ),
            "This MVP records recoveries only for above-band pricing"
        );

        ComplianceEvaluation evaluation =
            Checks.found(
                evaluations.findById(
                    exception.getEvaluationId()
                ),
                "Evaluation not found"
            );

        Long potential =
            evaluation.getPotentialPriceLeakageScaled();

        Checks.require(
            potential != null && potential > 0,
            "Evaluation has no positive quantified leakage"
        );

        Checks.require(
            request.currency().equals(
                evaluation.getComparisonCurrency()
            ),
            "Recovery currency must match the evaluation currency"
        );

        long existing = 0L;

        for (RecoveryRecord record :
                recoveries.findByExceptionId(id)) {
            if (request.recordType().equals(
                    record.getRecordType())) {
                existing = Math.addExact(
                    existing,
                    record.getAmountScaled()
                );
            }
        }

        long proposed = Math.addExact(
            existing,
            request.amountScaled()
        );

        Checks.conflict(
            proposed > potential,
            "Cumulative amount for this record type "
                + "exceeds the evaluation's potential leakage"
        );

        RecoveryRecord record =
            new RecoveryRecord();

        record.setExceptionId(id);
        record.setRecordedByUserId(actorId);
        record.setRecordType(
            request.recordType()
        );
        record.setAmountScaled(
            request.amountScaled()
        );
        record.setCurrency(
            request.currency()
        );
        record.setEvidenceReference(
            request.evidenceReference().trim()
        );

        record = recoveries.saveAndFlush(record);

        recordEvent(
            exception,
            actorId,
            access.importRole(actorId),
            "RECORD_" + request.recordType(),
            "Recovery record "
                + record.getRecoveryId()
                + " recorded with an evidence reference."
        );

        log.info(
            "Recovery record added; recoveryId={}, exceptionId={}, actorId={}",
            record.getRecoveryId(), id, actorId
        );

        return ResponseMapper.toResponse(record);
    }

    private ProcurementException get(Long id) {
        return Checks.found(
            exceptions.findById(id),
            "Exception not found"
        );
    }

    private void requireExplanation(
        ProcurementException exception
    ) {
        Checks.conflict(
            !"EXPLANATION_SUBMITTED".equals(
                exception.getReviewStatus()
            ),
            "A buyer explanation is required before a final decision"
        );
    }

    private void recordEvent(
        ProcurementException exception,
        Long actorId,
        String role,
        String action,
        String comment
    ) {
        ReviewEvent event = new ReviewEvent();

        event.setExceptionId(
            exception.getExceptionId()
        );
        event.setActorUserId(actorId);
        event.setActorRoleAtAction(role);
        event.setAction(action);
        event.setComment(comment);

        events.saveAndFlush(event);
    }
}
