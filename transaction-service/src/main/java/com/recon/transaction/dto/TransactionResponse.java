package com.recon.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        String transactionId,
        String productId,
        String accountId,
        BigDecimal quantity,
        BigDecimal price,
        String currency,
        String status,
        Instant tradeDate,
        Instant createdAt
) {}
