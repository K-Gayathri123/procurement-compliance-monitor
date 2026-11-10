package com.procurement.monitor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReviewDecisionRequest(
    @NotBlank
    @Pattern(
        regexp = "REQUEST_EVIDENCE|APPROVE_EXCEPTION|CONFIRM_VIOLATION"
    )
    String action,

    @NotBlank @Size(max = 4000) String comment
) {}
