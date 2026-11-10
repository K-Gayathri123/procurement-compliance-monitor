package com.procurement.monitor.dto.request;

import jakarta.validation.constraints.*;

public record SupplierRequest(
    @NotBlank @Size(max = 200) String canonicalName
) {}
