package com.procurement.monitor.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "suppliers")
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "canonical_name", nullable = false)
    private String canonicalName;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

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

    public Supplier() {}

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long value) {
        this.supplierId = value;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public void setCanonicalName(String value) {
        this.canonicalName = value;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean value) {
        this.active = value;
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

}
