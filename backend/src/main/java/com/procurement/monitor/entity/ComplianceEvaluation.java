package com.procurement.monitor.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "compliance_evaluations")
public class ComplianceEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_id", nullable = false)
    private Long evaluationId;

    @Column(name = "po_line_id", nullable = false)
    private Long poLineId;

    @Column(name = "contract_id", nullable = true)
    private Long contractId;

    @Column(name = "tier_id", nullable = true)
    private Long tierId;

    @Column(name = "rate_id", nullable = true)
    private Long rateId;

    @Column(name = "evaluated_at", nullable = false)
    private String evaluatedAt = java.time.Instant.now().toString();

    @Column(name = "rule_version", nullable = false)
    private String ruleVersion;

    @Column(name = "result_status", nullable = false)
    private String resultStatus;

    @Column(name = "comparison_currency", nullable = true)
    private String comparisonCurrency;

    @Column(name = "converted_unit_price_scaled", nullable = true)
    private Long convertedUnitPriceScaled;

    @Column(name = "potential_price_leakage_scaled", nullable = true)
    private Long potentialPriceLeakageScaled;

    @Column(name = "matching_snapshot", nullable = false)
    private String matchingSnapshot = "{}";

    @Column(name = "explanation", nullable = false)
    private String explanation;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_id", insertable = false, updatable = false)
    private ExchangeRate rateIdReference;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id", insertable = false, updatable = false)
    private ContractPriceTier tierIdReference;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", insertable = false, updatable = false)
    private Contract contractIdReference;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_line_id", insertable = false, updatable = false)
    private PurchaseOrderLine poLineIdReference;

    public ComplianceEvaluation() {}

    public Long getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(Long value) {
        this.evaluationId = value;
    }

    public Long getPoLineId() {
        return poLineId;
    }

    public void setPoLineId(Long value) {
        this.poLineId = value;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long value) {
        this.contractId = value;
    }

    public Long getTierId() {
        return tierId;
    }

    public void setTierId(Long value) {
        this.tierId = value;
    }

    public Long getRateId() {
        return rateId;
    }

    public void setRateId(Long value) {
        this.rateId = value;
    }

    public String getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(String value) {
        this.evaluatedAt = value;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String value) {
        this.ruleVersion = value;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String value) {
        this.resultStatus = value;
    }

    public String getComparisonCurrency() {
        return comparisonCurrency;
    }

    public void setComparisonCurrency(String value) {
        this.comparisonCurrency = value;
    }

    public Long getConvertedUnitPriceScaled() {
        return convertedUnitPriceScaled;
    }

    public void setConvertedUnitPriceScaled(Long value) {
        this.convertedUnitPriceScaled = value;
    }

    public Long getPotentialPriceLeakageScaled() {
        return potentialPriceLeakageScaled;
    }

    public void setPotentialPriceLeakageScaled(Long value) {
        this.potentialPriceLeakageScaled = value;
    }

    public String getMatchingSnapshot() {
        return matchingSnapshot;
    }

    public void setMatchingSnapshot(String value) {
        this.matchingSnapshot = value;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String value) {
        this.explanation = value;
    }

    @JsonIgnore
    public ExchangeRate getRateIdReference() {
        return rateIdReference;
    }

    @JsonIgnore
    public ContractPriceTier getTierIdReference() {
        return tierIdReference;
    }

    @JsonIgnore
    public Contract getContractIdReference() {
        return contractIdReference;
    }

    @JsonIgnore
    public PurchaseOrderLine getPoLineIdReference() {
        return poLineIdReference;
    }

}
