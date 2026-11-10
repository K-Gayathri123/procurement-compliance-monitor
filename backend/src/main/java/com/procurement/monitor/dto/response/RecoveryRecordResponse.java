package com.procurement.monitor.dto.response;

public record RecoveryRecordResponse(
    Long recoveryId,
    Long exceptionId,
    Long recordedByUserId,
    String recordType,
    Long amountScaled,
    String currency,
    String evidenceReference,
    String recordedAt
) {}
