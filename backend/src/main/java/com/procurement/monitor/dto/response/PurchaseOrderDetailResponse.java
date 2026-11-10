package com.procurement.monitor.dto.response;

import java.util.List;

public record PurchaseOrderDetailResponse(
    PurchaseOrderResponse order,
    List<PurchaseOrderLineResponse> lines
) {}
