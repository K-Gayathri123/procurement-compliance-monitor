package com.procurement.monitor.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "upload_batches")
public class UploadBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "upload_id", nullable = false)
    private Long uploadId;

    @Column(name = "uploaded_by_user_id", nullable = false)
    private Long uploadedByUserId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_hash", nullable = true)
    private String fileHash;

    @Column(name = "s3_object_key", nullable = true)
    private String s3ObjectKey;

    @Column(name = "uploaded_at", nullable = false)
    private String uploadedAt = java.time.Instant.now().toString();

    @Column(name = "processing_status", nullable = false)
    private String processingStatus = "PENDING";

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id", insertable = false, updatable = false)
    private UserAccount uploadedByUserIdReference;

    public UploadBatch() {}

    public Long getUploadId() {
        return uploadId;
    }

    public void setUploadId(Long value) {
        this.uploadId = value;
    }

    public Long getUploadedByUserId() {
        return uploadedByUserId;
    }

    public void setUploadedByUserId(Long value) {
        this.uploadedByUserId = value;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String value) {
        this.fileName = value;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String value) {
        this.fileHash = value;
    }

    public String getS3ObjectKey() {
        return s3ObjectKey;
    }

    public void setS3ObjectKey(String value) {
        this.s3ObjectKey = value;
    }

    public String getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(String value) {
        this.uploadedAt = value;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(String value) {
        this.processingStatus = value;
    }

    @JsonIgnore
    public UserAccount getUploadedByUserIdReference() {
        return uploadedByUserIdReference;
    }

}
