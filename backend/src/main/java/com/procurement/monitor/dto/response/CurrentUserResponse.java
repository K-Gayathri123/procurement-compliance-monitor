package com.procurement.monitor.dto.response;

import java.util.List;

public record CurrentUserResponse(
    Long userId,
    String fullName,
    String email,
    List<String> roles
) {}
