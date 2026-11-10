package com.procurement.monitor.dto.request;

import jakarta.validation.constraints.*;

public record ExchangeRateRequest(
    @NotBlank @Pattern(regexp = "[A-Z]{3}") String fromCurrency,
    @NotBlank @Pattern(regexp = "[A-Z]{3}") String toCurrency,
    @NotBlank String rateDate,
    @NotBlank @Size(max = 50) String conversionRate,
    @NotBlank @Size(max = 150) String rateSource
) {}
