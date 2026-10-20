package com.recon.transaction.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transaction_capture")
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 64)
    private String transactionId;

    @Column(name = "product_id", nullable = false, length = 32)
    private String productId;

    @Column(name = "account_id", nullable = false, length = 32)
    private String accountId;

    @Column(name = "quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "price", nullable = false, precision = 18, scale = 4)
    private BigDecimal price;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "trade_date", nullable = false)
    private Instant tradeDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public TransactionEntity() {}

    public TransactionEntity(String transactionId, String productId, String accountId,
                             BigDecimal quantity, BigDecimal price, String currency,
                             String status, Instant tradeDate) {
        this.transactionId = transactionId;
        this.productId = productId;
        this.accountId = accountId;
        this.quantity = quantity;
        this.price = price;
        this.currency = currency;
        this.status = status;
        this.tradeDate = tradeDate;
    }

    public Long getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getTradeDate() { return tradeDate; }
    public void setTradeDate(Instant tradeDate) { this.tradeDate = tradeDate; }
    public Instant getCreatedAt() { return createdAt; }
}
