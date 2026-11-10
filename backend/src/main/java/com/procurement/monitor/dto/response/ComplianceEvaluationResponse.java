package com.procurement.monitor.dto.response;

public record ComplianceEvaluationResponse(
    Long evaluationId,
    Long poLineId,
    Long contractId,
    Long tierId,
    Long rateId,
    String evaluatedAt,
    String ruleVersion,
    String resultStatus,
    String comparisonCurrency,
    Long convertedUnitPriceScaled,
    Long potentialPriceLeakageScaled,
    String matchingSnapshot,
    String explanation
) {}
