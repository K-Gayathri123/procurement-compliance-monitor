package com.procurement.monitor.dto.response;

public record SupplierAliasResponse(
    Long aliasId,
    Long supplierId,
    String aliasName,
    String normalizedAlias,
    String deletedAt,
    Long deletedByUserId,
    String deletionReason
) {}
