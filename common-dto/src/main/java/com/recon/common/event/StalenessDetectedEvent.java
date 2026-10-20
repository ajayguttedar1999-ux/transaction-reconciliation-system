package com.recon.common.event;

import java.time.Instant;
import java.time.LocalDate;

public record StalenessDetectedEvent(
        String eventId,
        String runId,
        LocalDate reconDate,
        String triggeredByTransactionId,
        String productId,
        String reason,
        Instant detectedAt
) {}
