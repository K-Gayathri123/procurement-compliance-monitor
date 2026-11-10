package com.procurement.monitor.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "recovery_records")
public class RecoveryRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recovery_id", nullable = false)
    private Long recoveryId;

    @Column(name = "exception_id", nullable = false)
    private Long exceptionId;

    @Column(name = "recorded_by_user_id", nullable = false)
    private Long recordedByUserId;

    @Column(name = "record_type", nullable = false)
    private String recordType;

    @Column(name = "amount_scaled", nullable = false)
    private Long amountScaled;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "evidence_reference", nullable = false)
    private String evidenceReference;

    @Column(name = "recorded_at", nullable = false)
    private String recordedAt = java.time.Instant.now().toString();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_user_id", insertable = false, updatable = false)
    private UserAccount recordedByUserIdReference;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exception_id", insertable = false, updatable = false)
    private ProcurementException exceptionIdReference;

    public RecoveryRecord() {}

    public Long getRecoveryId() {
        return recoveryId;
    }

    public void setRecoveryId(Long value) {
        this.recoveryId = value;
    }

    public Long getExceptionId() {
        return exceptionId;
    }

    public void setExceptionId(Long value) {
        this.exceptionId = value;
    }

    public Long getRecordedByUserId() {
        return recordedByUserId;
    }

    public void setRecordedByUserId(Long value) {
        this.recordedByUserId = value;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String value) {
        this.recordType = value;
    }

    public Long getAmountScaled() {
        return amountScaled;
    }

    public void setAmountScaled(Long value) {
        this.amountScaled = value;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String value) {
        this.currency = value;
    }

    public String getEvidenceReference() {
        return evidenceReference;
    }

    public void setEvidenceReference(String value) {
        this.evidenceReference = value;
    }

    public String getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(String value) {
        this.recordedAt = value;
    }

    @JsonIgnore
    public UserAccount getRecordedByUserIdReference() {
        return recordedByUserIdReference;
    }

    @JsonIgnore
    public ProcurementException getExceptionIdReference() {
        return exceptionIdReference;
    }

}
