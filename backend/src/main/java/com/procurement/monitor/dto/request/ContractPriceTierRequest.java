package com.procurement.monitor.dto.request;

import jakarta.validation.constraints.*;

public record ContractPriceTierRequest(
    @NotNull @Positive Long contractId,
    @NotNull @Positive Long minQuantity,
    @Positive Long maxQuantity,
    @NotNull @PositiveOrZero Long referenceUnitPriceScaled,
    @NotNull @PositiveOrZero Long allowedMinPriceScaled,
    @NotNull @PositiveOrZero Long allowedMaxPriceScaled
) {}
