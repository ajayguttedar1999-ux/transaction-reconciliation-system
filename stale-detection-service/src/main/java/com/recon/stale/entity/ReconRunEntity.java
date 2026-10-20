package com.recon.stale.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

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
    private boolean isStale;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ReconRunEntity() {}

    public Long getId() { return id; }
    public String getRunId() { return runId; }
    public LocalDate getReconDate() { return reconDate; }
    public String getStatus() { return status; }
    public boolean isStale() { return isStale; }
    public void setStale(boolean stale) { isStale = stale; }
    public Instant getCreatedAt() { return createdAt; }
}
