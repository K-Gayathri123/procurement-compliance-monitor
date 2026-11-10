package com.procurement.monitor.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "users")
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", nullable = true)
    private String passwordHash;

    @Column(name = "active", nullable = false)
    private Boolean active = false;

    @Column(name = "created_at", nullable = false)
    private String createdAt = java.time.Instant.now().toString();

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

    public UserAccount() {}

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long value) {
        this.userId = value;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String value) {
        this.fullName = value;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String value) {
        this.email = value;
    }

    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String value) {
        this.passwordHash = value;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean value) {
        this.active = value;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String value) {
        this.createdAt = value;
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
