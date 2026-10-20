package com.recon.common.event;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionBookedEvent(
        String eventId,
        String transactionId,
        String productId,
        String accountId,
        BigDecimal quantity,
        BigDecimal price,
        String currency,
        String status,
        Instant tradeDate,
        Instant bookedAt
) {}
