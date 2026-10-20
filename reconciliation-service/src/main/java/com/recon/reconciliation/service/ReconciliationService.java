package com.recon.reconciliation.service;

import com.recon.common.event.ReconciliationCompletedEvent;
import com.recon.reconciliation.entity.ReconBalanceResultEntity;
import com.recon.reconciliation.entity.ReconRunEntity;
import com.recon.reconciliation.producer.ReconciliationEventProducer;
import com.recon.reconciliation.repository.ReconBalanceResultRepository;
import com.recon.reconciliation.repository.ReconRunRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final ReconRunRepository runRepository;
    private final ReconBalanceResultRepository resultRepository;
    private final ReconciliationEventProducer eventProducer;
    private final EntityManager entityManager;

    public ReconciliationService(
            ReconRunRepository runRepository,
            ReconBalanceResultRepository resultRepository,
            ReconciliationEventProducer eventProducer,
            EntityManager entityManager) {
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.eventProducer = eventProducer;
        this.entityManager = entityManager;
    }

    @Transactional
    public ReconRunEntity runReconciliation(LocalDate reconDate) {
        String runId = "RUN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Starting reconciliation: runId={}", runId);

        @SuppressWarnings("unchecked")
        List<Object[]> txnAggregates = entityManager.createNativeQuery(
                "SELECT product_id, SUM(quantity) FROM transaction_capture GROUP BY product_id"
        ).getResultList();

        Map<String, BigDecimal> txnTotals = new HashMap<>();
        for (Object[] row : txnAggregates) {
            String prod = String.valueOf(row[0]);
            BigDecimal qty = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            txnTotals.put(prod, qty);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> posAggregates = entityManager.createNativeQuery(
                "SELECT product_id, total_quantity FROM position_capture"
        ).getResultList();

        Map<String, BigDecimal> posTotals = new HashMap<>();
        for (Object[] row : posAggregates) {
            String prod = String.valueOf(row[0]);
            BigDecimal qty = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            posTotals.put(prod, qty);
        }

        Set<String> allProducts = new HashSet<>();
        allProducts.addAll(txnTotals.keySet());
        allProducts.addAll(posTotals.keySet());

        ReconRunEntity runEntity = new ReconRunEntity(runId, reconDate, "BALANCED");
        runEntity = runRepository.save(runEntity);

        boolean hasImbalance = false;
        int imbalancedCount = 0;

        for (String product : allProducts) {
            BigDecimal txnQty = txnTotals.getOrDefault(product, BigDecimal.ZERO);
            BigDecimal posQty = posTotals.getOrDefault(product, BigDecimal.ZERO);
            BigDecimal discrepancy = txnQty.subtract(posQty);

            boolean isBalanced = discrepancy.compareTo(BigDecimal.ZERO) == 0;
            String status = isBalanced ? "BALANCED" : "IMBALANCED";

            if (!isBalanced) {
                hasImbalance = true;
                imbalancedCount++;
            }

            ReconBalanceResultEntity result = new ReconBalanceResultEntity(
                    runEntity, product, txnQty, posQty, discrepancy, status
            );
            resultRepository.save(result);
            runEntity.addResult(result);
        }

        if (hasImbalance) {
            runEntity.setStatus("IMBALANCED");
        }
        runRepository.save(runEntity);

        ReconciliationCompletedEvent event = new ReconciliationCompletedEvent(
                UUID.randomUUID().toString(),
                runEntity.getRunId(),
                runEntity.getReconDate(),
                runEntity.getStatus(),
                allProducts.size(),
                imbalancedCount,
                Instant.now()
        );
        eventProducer.publishReconciliationCompleted(event);

        return runEntity;
    }

    @Transactional(readOnly = true)
    public List<ReconRunEntity> getAllRuns() {
        return runRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<ReconRunEntity> getRunById(String runId) {
        return runRepository.findByRunId(runId);
    }
}
