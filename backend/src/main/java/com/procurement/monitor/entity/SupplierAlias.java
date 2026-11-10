package com.procurement.monitor.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "supplier_aliases")
public class SupplierAlias {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alias_id", nullable = false)
    private Long aliasId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "alias_name", nullable = false)
    private String aliasName;

    @Column(name = "normalized_alias", nullable = false)
    private String normalizedAlias;

    @Column(name = "deleted_at", nullable = true)
    private String deletedAt;

    @Column(name = "deleted_by_user_id", nullable = true)
    private Long deletedByUserId;

    @Column(name = "deletion_reason", nullable = true)
    private String deletionReason;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_user_id", insertable = false, updatable = false)
    private UserAccount deletedByUserIdReference;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", insertable = false, updatable = false)
    private Supplier supplierIdReference;

    public SupplierAlias() {}

    public Long getAliasId() {
        return aliasId;
    }

    public void setAliasId(Long value) {
        this.aliasId = value;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long value) {
        this.supplierId = value;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String value) {
        this.aliasName = value;
    }

    public String getNormalizedAlias() {
        return normalizedAlias;
    }

    public void setNormalizedAlias(String value) {
        this.normalizedAlias = value;
    }

    public String getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(String value) {
        this.deletedAt = value;
    }

    public Long getDeletedByUserId() {
        return deletedByUserId;
    }

    public void setDeletedByUserId(Long value) {
        this.deletedByUserId = value;
    }

    public String getDeletionReason() {
        return deletionReason;
    }

    public void setDeletionReason(String value) {
        this.deletionReason = value;
    }

    @JsonIgnore
    public UserAccount getDeletedByUserIdReference() {
        return deletedByUserIdReference;
    }

    @JsonIgnore
    public Supplier getSupplierIdReference() {
        return supplierIdReference;
    }

}
