package com.procurement.monitor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActionRequest(
    @NotBlank @Size(max = 2000) String reason
) {}
