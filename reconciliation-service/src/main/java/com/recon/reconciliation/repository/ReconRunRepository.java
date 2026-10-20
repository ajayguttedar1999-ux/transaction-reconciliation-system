package com.recon.reconciliation.repository;

import com.recon.reconciliation.entity.ReconRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReconRunRepository extends JpaRepository<ReconRunEntity, Long> {
    Optional<ReconRunEntity> findByRunId(String runId);
}
