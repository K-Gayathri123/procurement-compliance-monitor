package com.procurement.monitor.dto.request;

import jakarta.validation.constraints.*;

public record ContractRequest(
    @NotNull @Positive Long supplierId,
    @NotBlank @Size(max = 100) String contractNumber,
    @NotBlank @Size(max = 100) String itemCode,
    @NotBlank String effectiveFrom,
    @NotBlank String effectiveTo,
    @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency
) {}
