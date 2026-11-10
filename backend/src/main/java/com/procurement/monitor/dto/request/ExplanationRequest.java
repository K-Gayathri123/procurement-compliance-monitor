package com.procurement.monitor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExplanationRequest(
    @NotBlank @Size(max = 4000) String comment
) {}
