package com.procurement.monitor.controller;

import com.procurement.monitor.dto.request.*;
import com.procurement.monitor.dto.response.*;
import com.procurement.monitor.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exceptions")
@Validated
@PreAuthorize(
    "hasAnyRole('ADMIN','PROCUREMENT_OPERATOR','BUYER','REVIEWER','ANALYST')"
)
public class ProcurementExceptionController {

    private final OperationalQueryService queries;
    private final ReviewWorkflowService workflow;

    public ProcurementExceptionController(
        OperationalQueryService queries,
        ReviewWorkflowService workflow
    ) {
        this.queries = queries;
        this.workflow = workflow;
    }

    @GetMapping
    public PageResponse<ProcurementExceptionResponse> list(
        @RequestParam(defaultValue = "0")
        @Min(0) int page,

        @RequestParam(defaultValue = "20")
        @Min(1) @Max(100) int size,

        @RequestParam(required = false)
        @Size(max = 100) String status,

        @RequestParam(required = false)
        @Size(max = 100) String type,

        @AuthenticationPrincipal Jwt jwt
    ) {
        return queries.exceptions(
            page, size, status, type,
            Long.valueOf(jwt.getSubject())
        );
    }

    @GetMapping("/{id}")
    public ProcurementExceptionResponse get(
        @PathVariable @Positive Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return queries.exception(
            id,
            Long.valueOf(jwt.getSubject())
        );
    }

    @PostMapping("/{id}/explanations")
    @PreAuthorize("hasAnyRole('ADMIN','BUYER')")
    public ProcurementExceptionResponse explain(
        @PathVariable @Positive Long id,
        @Valid @RequestBody ExplanationRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return workflow.explain(
            id, request,
            Long.valueOf(jwt.getSubject())
        );
    }

    @PostMapping("/{id}/decisions")
    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    public ProcurementExceptionResponse decide(
        @PathVariable @Positive Long id,
        @Valid @RequestBody ReviewDecisionRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return workflow.decide(
            id, request,
            Long.valueOf(jwt.getSubject())
        );
    }

    @GetMapping("/{id}/review-events")
    public List<ReviewEventResponse> history(
        @PathVariable @Positive Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return workflow.history(
            id,
            Long.valueOf(jwt.getSubject())
        );
    }

    @GetMapping("/{id}/recoveries")
    public List<RecoveryRecordResponse> recoveries(
        @PathVariable @Positive Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return workflow.recoveries(
            id,
            Long.valueOf(jwt.getSubject())
        );
    }

    @PostMapping("/{id}/recoveries")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OPERATOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public RecoveryRecordResponse recordRecovery(
        @PathVariable @Positive Long id,
        @Valid @RequestBody RecoveryRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return workflow.recordRecovery(
            id, request,
            Long.valueOf(jwt.getSubject())
        );
    }
}
