package com.procurement.monitor.dto.response;

public record UploadProcessingResponse(
    UploadBatchResponse upload,
    int purchaseOrdersCreated,
    int purchaseLinesCreated,
    int evaluationsCreated,
    long exceptionsCreated
) {}
