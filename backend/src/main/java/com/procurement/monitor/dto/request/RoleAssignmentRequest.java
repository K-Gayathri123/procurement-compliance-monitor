package com.procurement.monitor.dto.request;

import jakarta.validation.constraints.*;
import java.util.Set;

public record RoleAssignmentRequest(
    @NotEmpty @Size(max = 10)
    Set<@NotNull @Positive Long> roleIds
) {}
