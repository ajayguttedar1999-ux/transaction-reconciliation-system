package com.recon.common.event;

import java.math.BigDecimal;
import java.time.Instant;

public record PositionCreatedEvent(
        String eventId,
        String positionId,
        String productId,
        BigDecimal totalQuantity,
        BigDecimal totalAmount,
        int version,
        Instant updatedAt
) {}
