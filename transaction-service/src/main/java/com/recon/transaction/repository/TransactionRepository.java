package com.recon.transaction.repository;

import com.recon.transaction.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    Optional<TransactionEntity> findByTransactionId(String transactionId);
    boolean existsByTransactionId(String transactionId);
}
