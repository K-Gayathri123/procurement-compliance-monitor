package com.procurement.monitor.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "purchase_order_lines")
public class PurchaseOrderLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "po_line_id", nullable = false)
    private Long poLineId;

    @Column(name = "po_id", nullable = false)
    private Long poId;

    @Column(name = "line_number", nullable = false)
    private Long lineNumber;

    @Column(name = "item_code", nullable = false)
    private String itemCode;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Column(name = "actual_unit_price_scaled", nullable = false)
    private Long actualUnitPriceScaled;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", insertable = false, updatable = false)
    private PurchaseOrder poIdReference;

    public PurchaseOrderLine() {}

    public Long getPoLineId() {
        return poLineId;
    }

    public void setPoLineId(Long value) {
        this.poLineId = value;
    }

    public Long getPoId() {
        return poId;
    }

    public void setPoId(Long value) {
        this.poId = value;
    }

    public Long getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Long value) {
        this.lineNumber = value;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String value) {
        this.itemCode = value;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long value) {
        this.quantity = value;
    }

    public Long getActualUnitPriceScaled() {
        return actualUnitPriceScaled;
    }

    public void setActualUnitPriceScaled(Long value) {
        this.actualUnitPriceScaled = value;
    }

    @JsonIgnore
    public PurchaseOrder getPoIdReference() {
        return poIdReference;
    }

}
