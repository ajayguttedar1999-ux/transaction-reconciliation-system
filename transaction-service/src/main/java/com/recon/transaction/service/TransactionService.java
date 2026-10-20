package com.recon.transaction.service;

import com.recon.common.event.TransactionBookedEvent;
import com.recon.transaction.dto.CreateTransactionRequest;
import com.recon.transaction.dto.TransactionResponse;
import com.recon.transaction.entity.TransactionEntity;
import com.recon.transaction.producer.TransactionEventProducer;
import com.recon.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository repository;
    private final TransactionEventProducer producer;

    public TransactionService(TransactionRepository repository, TransactionEventProducer producer) {
        this.repository = repository;
        this.producer = producer;
    }

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest req) {
        if (repository.existsByTransactionId(req.transactionId())) {
            throw new IllegalArgumentException("Transaction with ID " + req.transactionId() + " already exists!");
        }

        TransactionEntity entity = new TransactionEntity(
                req.transactionId(),
                req.productId(),
                req.accountId(),
                req.quantity(),
                req.price(),
                req.currency(),
                "BOOKED",
                req.tradeDate()
        );

        TransactionEntity saved = repository.save(entity);
        log.info("Transaction saved: id={}, txnId={}", saved.getId(), saved.getTransactionId());

        TransactionBookedEvent event = new TransactionBookedEvent(
                UUID.randomUUID().toString(),
                saved.getTransactionId(),
                saved.getProductId(),
                saved.getAccountId(),
                saved.getQuantity(),
                saved.getPrice(),
                saved.getCurrency(),
                saved.getStatus(),
                saved.getTradeDate(),
                Instant.now()
        );
        producer.publishTransactionBooked(event);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAllTransactions() {
        return repository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private TransactionResponse mapToResponse(TransactionEntity e) {
        return new TransactionResponse(
                e.getTransactionId(),
                e.getProductId(),
                e.getAccountId(),
                e.getQuantity(),
                e.getPrice(),
                e.getCurrency(),
                e.getStatus(),
                e.getTradeDate(),
                e.getCreatedAt()
        );
    }
}
