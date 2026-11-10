package com.procurement.monitor.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "review_events")
public class ReviewEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_event_id", nullable = false)
    private Long reviewEventId;

    @Column(name = "exception_id", nullable = false)
    private Long exceptionId;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(name = "actor_role_at_action", nullable = false)
    private String actorRoleAtAction;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "comment", nullable = false)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private String createdAt = java.time.Instant.now().toString();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id", insertable = false, updatable = false)
    private UserAccount actorUserIdReference;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exception_id", insertable = false, updatable = false)
    private ProcurementException exceptionIdReference;

    public ReviewEvent() {}

    public Long getReviewEventId() {
        return reviewEventId;
    }

    public void setReviewEventId(Long value) {
        this.reviewEventId = value;
    }

    public Long getExceptionId() {
        return exceptionId;
    }

    public void setExceptionId(Long value) {
        this.exceptionId = value;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long value) {
        this.actorUserId = value;
    }

    public String getActorRoleAtAction() {
        return actorRoleAtAction;
    }

    public void setActorRoleAtAction(String value) {
        this.actorRoleAtAction = value;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String value) {
        this.action = value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String value) {
        this.comment = value;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String value) {
        this.createdAt = value;
    }

    @JsonIgnore
    public UserAccount getActorUserIdReference() {
        return actorUserIdReference;
    }

    @JsonIgnore
    public ProcurementException getExceptionIdReference() {
        return exceptionIdReference;
    }

}
