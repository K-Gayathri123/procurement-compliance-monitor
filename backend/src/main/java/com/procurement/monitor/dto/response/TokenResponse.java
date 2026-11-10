package com.procurement.monitor.dto.response;

public record TokenResponse(
    String accessToken,
    String tokenType,
    long expiresIn
) {}
