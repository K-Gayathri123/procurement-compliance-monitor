package com.procurement.monitor.dto.response;

public record ProcurementExceptionResponse(
    Long exceptionId,
    Long evaluationId,
    String exceptionType,
    String relatedGroupKey,
    String reviewStatus,
    String reason,
    String createdAt
) {}
