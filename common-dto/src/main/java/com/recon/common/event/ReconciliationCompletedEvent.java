package com.recon.common.event;

import java.time.Instant;
import java.time.LocalDate;

public record ReconciliationCompletedEvent(
        String eventId,
        String runId,
        LocalDate reconDate,
        String overallStatus,
        int totalProductsReconciled,
        int imbalancedCount,
        Instant completedAt
) {}
