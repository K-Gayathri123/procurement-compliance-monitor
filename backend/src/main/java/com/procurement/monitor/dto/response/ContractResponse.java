package com.procurement.monitor.dto.response;

public record ContractResponse(
    Long contractId,
    Long supplierId,
    String contractNumber,
    String itemCode,
    String effectiveFrom,
    String effectiveTo,
    String currency,
    String status
) {}
