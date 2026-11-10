package com.procurement.monitor.dto.response;

public record ContractPriceTierResponse(
    Long tierId,
    Long contractId,
    Long minQuantity,
    Long maxQuantity,
    Long referenceUnitPriceScaled,
    Long allowedMinPriceScaled,
    Long allowedMaxPriceScaled
) {}
