package com.recon.position.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "position_capture")
public class PositionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "position_id", nullable = false, unique = true, length = 64)
    private String positionId;

    @Column(name = "product_id", nullable = false, unique = true, length = 32)
    private String productId;

    @Column(name = "total_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalQuantity;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalAmount;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PositionEntity() {}

    public PositionEntity(String positionId, String productId, BigDecimal totalQuantity, BigDecimal totalAmount) {
        this.positionId = positionId;
        this.productId = productId;
        this.totalQuantity = totalQuantity;
        this.totalAmount = totalAmount;
        this.updatedAt = Instant.now();
    }

    public void addTransaction(BigDecimal quantity, BigDecimal amount) {
        this.totalQuantity = this.totalQuantity.add(quantity);
        this.totalAmount = this.totalAmount.add(amount);
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getPositionId() { return positionId; }
    public void setPositionId(String positionId) { this.positionId = positionId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal totalQuantity) { this.totalQuantity = totalQuantity; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Integer getVersion() { return version; }
    public Instant getUpdatedAt() { return updatedAt; }
}
