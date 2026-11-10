package com.procurement.monitor.dto.response;

public record PurchaseOrderResponse(
    Long poId,
    Long uploadId,
    Long supplierId,
    Long buyerUserId,
    String sourcePoNumber,
    String originalSupplierName,
    String department,
    String orderDate,
    String currency
) {}
