package com.procurement.monitor.dto.response;

public record SupplierResponse(
    Long supplierId,
    String canonicalName,
    Boolean active,
    String deletedAt,
    Long deletedByUserId,
    String deletionReason
) {}
