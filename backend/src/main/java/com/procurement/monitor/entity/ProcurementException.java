package com.procurement.monitor.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "exceptions")
public class ProcurementException {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exception_id", nullable = false)
    private Long exceptionId;

    @Column(name = "evaluation_id", nullable = false)
    private Long evaluationId;

    @Column(name = "exception_type", nullable = false)
    private String exceptionType;

    @Column(name = "related_group_key", nullable = true)
    private String relatedGroupKey;

    @Column(name = "review_status", nullable = false)
    private String reviewStatus = "OPEN";

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private String createdAt = java.time.Instant.now().toString();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", insertable = false, updatable = false)
    private ComplianceEvaluation evaluationIdReference;

    public ProcurementException() {}

    public Long getExceptionId() {
        return exceptionId;
    }

    public void setExceptionId(Long value) {
        this.exceptionId = value;
    }

    public Long getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(Long value) {
        this.evaluationId = value;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String value) {
        this.exceptionType = value;
    }

    public String getRelatedGroupKey() {
        return relatedGroupKey;
    }

    public void setRelatedGroupKey(String value) {
        this.relatedGroupKey = value;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String value) {
        this.reviewStatus = value;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String value) {
        this.reason = value;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String value) {
        this.createdAt = value;
    }

    @JsonIgnore
    public ComplianceEvaluation getEvaluationIdReference() {
        return evaluationIdReference;
    }

}
