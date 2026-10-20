package com.recon.transaction.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_recovery_queue")
public class RecoveryTransactionQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "status", length = 20, nullable = false)
    private String status; // PENDING_RETRY, RETRIED, ABANDONED

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public RecoveryTransactionQueue() {}

    public RecoveryTransactionQueue(String transactionId, String productId, String payload, String failureReason) {
        this.transactionId = transactionId;
        this.productId = productId;
        this.payload = payload;
        this.failureReason = failureReason;
        this.status = "PENDING_RETRY";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public String getProductId() { return productId; }
    public String getPayload() { return payload; }
    public String getFailureReason() { return failureReason; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setStatus(String status) { this.status = status; }
}
