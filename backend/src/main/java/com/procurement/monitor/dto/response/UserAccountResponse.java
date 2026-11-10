package com.procurement.monitor.dto.response;

public record UserAccountResponse(
    Long userId,
    String fullName,
    String email,
    Boolean active,
    String createdAt,
    String deletedAt,
    Long deletedByUserId,
    String deletionReason
) {}
