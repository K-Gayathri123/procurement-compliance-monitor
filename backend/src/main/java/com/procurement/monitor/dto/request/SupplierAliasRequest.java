package com.procurement.monitor.dto.request;

import jakarta.validation.constraints.*;

public record SupplierAliasRequest(
    @NotNull @Positive Long supplierId,
    @NotBlank @Size(max = 200) String aliasName
) {}
