package com.recon.reconciliation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "recon_balance_result")
public class ReconBalanceResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recon_run_id", referencedColumnName = "run_id", nullable = false)
    @JsonIgnore
    private ReconRunEntity reconRun;

    @Column(name = "product_id", nullable = false, length = 32)
    private String productId;

    @Column(name = "txn_total_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal txnTotalQty;

    @Column(name = "pos_total_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal posTotalQty;

    @Column(name = "discrepancy_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal discrepancyQty;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ReconBalanceResultEntity() {}

    public ReconBalanceResultEntity(ReconRunEntity reconRun, String productId,
                                  BigDecimal txnTotalQty, BigDecimal posTotalQty,
                                  BigDecimal discrepancyQty, String status) {
        this.reconRun = reconRun;
        this.productId = productId;
        this.txnTotalQty = txnTotalQty;
        this.posTotalQty = posTotalQty;
        this.discrepancyQty = discrepancyQty;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public ReconRunEntity getReconRun() { return reconRun; }
    public void setReconRun(ReconRunEntity reconRun) { this.reconRun = reconRun; }
    public String getProductId() { return productId; }
    public BigDecimal getTxnTotalQty() { return txnTotalQty; }
    public BigDecimal getPosTotalQty() { return posTotalQty; }
    public BigDecimal getDiscrepancyQty() { return discrepancyQty; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
