package com.recon.stale.service;

import com.recon.common.event.StalenessDetectedEvent;
import com.recon.common.event.TransactionBookedEvent;
import com.recon.stale.entity.ReconRunEntity;
import com.recon.stale.producer.StalenessEventProducer;
import com.recon.stale.repository.StaleRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class StaleDetectionService {

    private static final Logger log = LoggerFactory.getLogger(StaleDetectionService.class);

    private final StaleRunRepository repository;
    private final StalenessEventProducer producer;

    public StaleDetectionService(StaleRunRepository repository, StalenessEventProducer producer) {
        this.repository = repository;
        this.producer = producer;
    }

    @Transactional
    public void evaluateTransactionForStaleness(TransactionBookedEvent event) {
        LocalDate tradeDate = event.tradeDate().atZone(ZoneOffset.UTC).toLocalDate();

        List<ReconRunEntity> activeRuns = repository.findByReconDateAndIsStaleFalse(tradeDate);

        for (ReconRunEntity run : activeRuns) {
            if (event.bookedAt().isAfter(run.getCreatedAt())) {
                log.warn("Late-arriving transaction [{}] detected! Invalidation run: {}", event.transactionId(), run.getRunId());
                run.setStale(true);
                repository.save(run);

                StalenessDetectedEvent staleEvent = new StalenessDetectedEvent(
                        UUID.randomUUID().toString(),
                        run.getRunId(),
                        run.getReconDate(),
                        event.transactionId(),
                        event.productId(),
                        "Late transaction arrived after reconciliation run completed",
                        Instant.now()
                );
                producer.publishStalenessDetected(staleEvent);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ReconRunEntity> getStaleRuns() {
        return repository.findByIsStaleTrue();
    }
}
