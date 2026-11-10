package com.procurement.monitor.dto.response;

public record ExchangeRateResponse(
    Long rateId,
    String fromCurrency,
    String toCurrency,
    String rateDate,
    String conversionRate,
    String rateSource
) {}
