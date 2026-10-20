package com.recon.cache.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * High-speed cached position representation stored inside Redis.
 */
public record CachedPosition(
        String positionId,
        String productId,
        BigDecimal totalQuantity,
        BigDecimal totalAmount,
        int version,
        Instant cachedAt
) {}
