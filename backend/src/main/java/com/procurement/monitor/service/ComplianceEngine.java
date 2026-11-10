package com.procurement.monitor.service;

import com.procurement.monitor.entity.*;
import com.procurement.monitor.repository.*;
import com.procurement.monitor.validation.PurchaseRules;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ComplianceEngine {

    private final ContractRepository contracts;
    private final ContractPriceTierRepository tiers;
    private final ExchangeRateRepository rates;
    private final ComplianceEvaluationRepository evaluations;
    private final ExceptionWriter exceptionWriter;
    private final ObjectMapper json;
    private final String preferredRateSource;

    public ComplianceEngine(ContractRepository contracts, ContractPriceTierRepository tiers, ExchangeRateRepository rates, ComplianceEvaluationRepository evaluations, ExceptionWriter exceptionWriter, ObjectMapper json, @Value("${app.compliance.rate-source:}") String preferredRateSource) {
        this.contracts = contracts;
        this.tiers = tiers;
        this.rates = rates;
        this.evaluations = evaluations;
        this.exceptionWriter = exceptionWriter;
        this.json = json;
        this.preferredRateSource = preferredRateSource.trim();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ComplianceEvaluation evaluate(PurchaseOrder order, PurchaseOrderLine line, Long actorId, String actorRole) {
        var evaluation = new ComplianceEvaluation();
        evaluation.setPoLineId(line.getPoLineId());
        evaluation.setRuleVersion("PROCUREMENT_V1");
        evaluation.setResultStatus("NEEDS_REVIEW");

        Map<String, Object> snapshot = new LinkedHashMap<>();

        snapshot.put("ruleVersion", "PROCUREMENT_V1");
        snapshot.put("poNumber", order.getSourcePoNumber());
        snapshot.put("orderDate", order.getOrderDate());
        snapshot.put("supplierId", order.getSupplierId());
        snapshot.put("originalSupplierName", order.getOriginalSupplierName());
        snapshot.put("itemCode", line.getItemCode());
        snapshot.put("quantity", line.getQuantity());
        snapshot.put("orderCurrency", order.getCurrency());
        snapshot.put("actualUnitPriceScaled", line.getActualUnitPriceScaled());
        snapshot.put("moneyScale", 10000);
        snapshot.put("fxPolicy", "DIRECT_EXACT_ORDER_DATE_HALF_UP_UNIT_PRICE");
        snapshot.put("preferredRateSource", preferredRateSource);

        String exceptionType = null;
        String explanation;

        if (order.getSupplierId() == null) {
            exceptionType = "SUPPLIER_UNRESOLVED";
            explanation = "No available supplier alias matched the uploaded name.";

        } else {
            var applicable = contracts.findBySupplierId(order.getSupplierId()).stream().filter(contract -> "APPROVED".equals(contract.getStatus()) && line.getItemCode().equals(contract.getItemCode()) && contract.getEffectiveFrom().compareTo(order.getOrderDate()) <= 0 && contract.getEffectiveTo().compareTo(order.getOrderDate()) >= 0).toList();

            snapshot.put("candidateContractIds", applicable.stream().map(Contract::getContractId).toList());

            if (applicable.size() != 1) {
                exceptionType = applicable.isEmpty() ? "CONTRACT_NOT_FOUND" : "CONTRACT_AMBIGUOUS";

                explanation = "Expected exactly one approved supplier/item " + "contract applicable on the order date; found " + applicable.size() + ".";

            } else {
                Contract contract = applicable.getFirst();
                evaluation.setContractId(contract.getContractId());
                evaluation.setComparisonCurrency(contract.getCurrency());
                snapshot.put("contractNumber", contract.getContractNumber());
                snapshot.put("contractStatusAtEvaluation", contract.getStatus());
                snapshot.put("contractEffectiveFrom", contract.getEffectiveFrom());
                snapshot.put("contractEffectiveTo", contract.getEffectiveTo());
                snapshot.put("comparisonCurrency", contract.getCurrency());

                var matchingTiers = tiers.findByContractId(contract.getContractId()).stream().filter(tier -> tier.getMinQuantity() <= line.getQuantity() && (tier.getMaxQuantity() == null || tier.getMaxQuantity() >= line.getQuantity())).toList();

                if (matchingTiers.size() != 1) {
                    exceptionType = "TIER_NOT_UNIQUE";
                    explanation = "Expected exactly one quantity tier; found " + matchingTiers.size() + ".";

                } else {
                    ContractPriceTier tier = matchingTiers.getFirst();

                    evaluation.setTierId(tier.getTierId());

                    snapshot.put("tierMinQuantity", tier.getMinQuantity());
                    snapshot.put("tierMaxQuantity", tier.getMaxQuantity());
                    snapshot.put("referenceUnitPriceScaled", tier.getReferenceUnitPriceScaled());
                    snapshot.put("allowedMinPriceScaled", tier.getAllowedMinPriceScaled());
                    snapshot.put("allowedMaxPriceScaled", tier.getAllowedMaxPriceScaled());

                    Long converted = null;

                    if (order.getCurrency().equals(contract.getCurrency())) {
                        converted = line.getActualUnitPriceScaled();

                        snapshot.put("conversionRate", "1");
                        snapshot.put("rateSource", "SAME_CURRENCY");

                    } else {
                        var matchingRates = rates.findAll().stream().filter(rate -> rate.getFromCurrency().equals(order.getCurrency()) && rate.getToCurrency().equals(contract.getCurrency()) && rate.getRateDate().equals(order.getOrderDate()) && (preferredRateSource.isEmpty() || preferredRateSource.equals(rate.getRateSource()))).toList();

                        snapshot.put("candidateRateIds", matchingRates.stream().map(ExchangeRate::getRateId).toList());

                        if (matchingRates.size() != 1) {
                            exceptionType = matchingRates.isEmpty() ? "EXCHANGE_RATE_MISSING" : "EXCHANGE_RATE_AMBIGUOUS";

                        } else {
                            ExchangeRate rate = matchingRates.getFirst();
                            BigDecimal decimal = new BigDecimal(rate.getConversionRate());
                            if (decimal.signum() <= 0) {
                                throw new IllegalStateException("Reference exchange rate is invalid");
                            }
                            converted = PurchaseRules.convert(line.getActualUnitPriceScaled(), decimal);
                            evaluation.setRateId(rate.getRateId());
                            snapshot.put("conversionRate", rate.getConversionRate());
                            snapshot.put("rateDate", rate.getRateDate());
                            snapshot.put("rateSource", rate.getRateSource());
                        }
                    }

                    if (converted == null) {
                        explanation = "A unique direct exchange rate for the " + "exact order date was not available.";

                    } else {
                        evaluation.setConvertedUnitPriceScaled(converted);

                        // Ensure comparison-currency line totals fit Long.
                        Math.multiplyExact(converted, line.getQuantity());

                        if (converted > tier.getAllowedMaxPriceScaled()) {
                            exceptionType = "PRICE_ABOVE_BAND";

                            long difference = Math.subtractExact(converted, tier.getReferenceUnitPriceScaled());

                            evaluation.setPotentialPriceLeakageScaled(Math.multiplyExact(difference, line.getQuantity()));

                            explanation = "Converted unit price exceeds the " + "contract's allowed maximum.";

                        } else if (converted < tier.getAllowedMinPriceScaled()) {
                            exceptionType = "PRICE_BELOW_BAND";

                            evaluation.setPotentialPriceLeakageScaled(0L);

                            explanation = "Converted unit price is below the " + "allowed minimum; validate the " + "item, price, and contract match.";

                        } else {
                            evaluation.setResultStatus("COMPLIANT");
                            evaluation.setPotentialPriceLeakageScaled(0L);

                            explanation = "Supplier, date, item, quantity, " + "currency, and price band matched.";
                        }
                    }
                }
            }
        }

        snapshot.put("exceptionType", exceptionType);
        snapshot.put("convertedUnitPriceScaled", evaluation.getConvertedUnitPriceScaled());
        snapshot.put("potentialPriceLeakageScaled", evaluation.getPotentialPriceLeakageScaled());
        evaluation.setExplanation(explanation);
        evaluation.setMatchingSnapshot(json.writeValueAsString(snapshot));
        evaluation = evaluations.saveAndFlush(evaluation);
        if (exceptionType != null) {
            exceptionWriter.create(evaluation, exceptionType, null, explanation, actorId, actorRole);
        }
        return evaluation;
    }
}
