package com.procurement.monitor.dto.response;

public record UploadBatchResponse(
    Long uploadId,
    Long uploadedByUserId,
    String fileName,
    String fileHash,
    String s3ObjectKey,
    String uploadedAt,
    String processingStatus
) {}
