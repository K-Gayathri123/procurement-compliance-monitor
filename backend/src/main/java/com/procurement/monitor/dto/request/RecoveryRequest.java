package com.procurement.monitor.dto.request;

import jakarta.validation.constraints.*;

public record RecoveryRequest(
    @NotBlank
    @Pattern(regexp = "RECOVERABLE_ESTIMATE|ACTUAL_RECOVERY")
    String recordType,

    @NotNull @Positive Long amountScaled,

    @NotBlank @Pattern(regexp = "[A-Z]{3}")
    String currency,

    @NotBlank @Size(max = 1000)
    String evidenceReference
) {}
