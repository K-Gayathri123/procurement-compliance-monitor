package com.procurement.monitor.dto.response;

public record ReviewEventResponse(
    Long reviewEventId,
    Long exceptionId,
    Long actorUserId,
    String actorRoleAtAction,
    String action,
    String comment,
    String createdAt
) {}
