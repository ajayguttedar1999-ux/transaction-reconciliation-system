package com.recon.transaction.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class TransactionMetrics {

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, Counter> tradeCounters = new ConcurrentHashMap<>();

    public TransactionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementTradeCount(String productId) {
        tradeCounters.computeIfAbsent(productId, prod -> Counter.builder("recon_transactions_booked_total")
                .description("Total number of financial transactions booked")
                .tag("product", prod)
                .register(meterRegistry)
        ).increment();
    }
}
