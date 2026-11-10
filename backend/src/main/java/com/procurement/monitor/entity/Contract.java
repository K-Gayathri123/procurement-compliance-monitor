package com.procurement.monitor.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "contracts")
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "contract_number", nullable = false)
    private String contractNumber;

    @Column(name = "item_code", nullable = false)
    private String itemCode;

    @Column(name = "effective_from", nullable = false)
    private String effectiveFrom;

    @Column(name = "effective_to", nullable = false)
    private String effectiveTo;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "status", nullable = false)
    private String status = "APPROVED";

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", insertable = false, updatable = false)
    private Supplier supplierIdReference;

    public Contract() {}

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long value) {
        this.contractId = value;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long value) {
        this.supplierId = value;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(String value) {
        this.contractNumber = value;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String value) {
        this.itemCode = value;
    }

    public String getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(String value) {
        this.effectiveFrom = value;
    }

    public String getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(String value) {
        this.effectiveTo = value;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String value) {
        this.currency = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        this.status = value;
    }

    @JsonIgnore
    public Supplier getSupplierIdReference() {
        return supplierIdReference;
    }

}
