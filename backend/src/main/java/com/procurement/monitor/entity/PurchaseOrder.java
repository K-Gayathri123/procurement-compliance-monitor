package com.procurement.monitor.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "po_id", nullable = false)
    private Long poId;

    @Column(name = "upload_id", nullable = false)
    private Long uploadId;

    @Column(name = "supplier_id", nullable = true)
    private Long supplierId;

    @Column(name = "buyer_user_id", nullable = true)
    private Long buyerUserId;

    @Column(name = "source_po_number", nullable = false)
    private String sourcePoNumber;

    @Column(name = "original_supplier_name", nullable = false)
    private String originalSupplierName;

    @Column(name = "department", nullable = false)
    private String department;

    @Column(name = "order_date", nullable = false)
    private String orderDate;

    @Column(name = "currency", nullable = false)
    private String currency;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_user_id", insertable = false, updatable = false)
    private UserAccount buyerUserIdReference;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", insertable = false, updatable = false)
    private Supplier supplierIdReference;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_id", insertable = false, updatable = false)
    private UploadBatch uploadIdReference;

    public PurchaseOrder() {}

    public Long getPoId() {
        return poId;
    }

    public void setPoId(Long value) {
        this.poId = value;
    }

    public Long getUploadId() {
        return uploadId;
    }

    public void setUploadId(Long value) {
        this.uploadId = value;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long value) {
        this.supplierId = value;
    }

    public Long getBuyerUserId() {
        return buyerUserId;
    }

    public void setBuyerUserId(Long value) {
        this.buyerUserId = value;
    }

    public String getSourcePoNumber() {
        return sourcePoNumber;
    }

    public void setSourcePoNumber(String value) {
        this.sourcePoNumber = value;
    }

    public String getOriginalSupplierName() {
        return originalSupplierName;
    }

    public void setOriginalSupplierName(String value) {
        this.originalSupplierName = value;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String value) {
        this.department = value;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String value) {
        this.orderDate = value;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String value) {
        this.currency = value;
    }

    @JsonIgnore
    public UserAccount getBuyerUserIdReference() {
        return buyerUserIdReference;
    }

    @JsonIgnore
    public Supplier getSupplierIdReference() {
        return supplierIdReference;
    }

    @JsonIgnore
    public UploadBatch getUploadIdReference() {
        return uploadIdReference;
    }

}
