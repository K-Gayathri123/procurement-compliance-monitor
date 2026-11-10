package com.procurement.monitor.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "contract_price_tiers")
public class ContractPriceTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tier_id", nullable = false)
    private Long tierId;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "min_quantity", nullable = false)
    private Long minQuantity;

    @Column(name = "max_quantity", nullable = true)
    private Long maxQuantity;

    @Column(name = "reference_unit_price_scaled", nullable = false)
    private Long referenceUnitPriceScaled;

    @Column(name = "allowed_min_price_scaled", nullable = false)
    private Long allowedMinPriceScaled;

    @Column(name = "allowed_max_price_scaled", nullable = false)
    private Long allowedMaxPriceScaled;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", insertable = false, updatable = false)
    private Contract contractIdReference;

    public ContractPriceTier() {}

    public Long getTierId() {
        return tierId;
    }

    public void setTierId(Long value) {
        this.tierId = value;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long value) {
        this.contractId = value;
    }

    public Long getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(Long value) {
        this.minQuantity = value;
    }

    public Long getMaxQuantity() {
        return maxQuantity;
    }

    public void setMaxQuantity(Long value) {
        this.maxQuantity = value;
    }

    public Long getReferenceUnitPriceScaled() {
        return referenceUnitPriceScaled;
    }

    public void setReferenceUnitPriceScaled(Long value) {
        this.referenceUnitPriceScaled = value;
    }

    public Long getAllowedMinPriceScaled() {
        return allowedMinPriceScaled;
    }

    public void setAllowedMinPriceScaled(Long value) {
        this.allowedMinPriceScaled = value;
    }

    public Long getAllowedMaxPriceScaled() {
        return allowedMaxPriceScaled;
    }

    public void setAllowedMaxPriceScaled(Long value) {
        this.allowedMaxPriceScaled = value;
    }

    @JsonIgnore
    public Contract getContractIdReference() {
        return contractIdReference;
    }

}
