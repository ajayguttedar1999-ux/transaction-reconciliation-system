package com.recon.reconciliation.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recon_run")
public class ReconRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false, unique = true, length = 64)
    private String runId;

    @Column(name = "recon_date", nullable = false)
    private LocalDate reconDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "is_stale", nullable = false)
    private boolean isStale = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "reconRun", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReconBalanceResultEntity> results = new ArrayList<>();

    public ReconRunEntity() {}

    public ReconRunEntity(String runId, LocalDate reconDate, String status) {
        this.runId = runId;
        this.reconDate = reconDate;
        this.status = status;
        this.isStale = false;
        this.createdAt = Instant.now();
    }

    public void addResult(ReconBalanceResultEntity result) {
        results.add(result);
        result.setReconRun(this);
    }

    public Long getId() { return id; }
    public String getRunId() { return runId; }
    public LocalDate getReconDate() { return reconDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isStale() { return isStale; }
    public void setStale(boolean stale) { isStale = stale; }
    public Instant getCreatedAt() { return createdAt; }
    public List<ReconBalanceResultEntity> getResults() { return results; }
}
