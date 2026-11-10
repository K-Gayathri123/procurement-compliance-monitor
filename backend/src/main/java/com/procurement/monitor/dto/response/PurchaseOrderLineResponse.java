package com.procurement.monitor.dto.response;

public record PurchaseOrderLineResponse(
    Long poLineId,
    Long poId,
    Long lineNumber,
    String itemCode,
    Long quantity,
    Long actualUnitPriceScaled
) {}
