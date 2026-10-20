package com.recon.reconciliation.controller;

import com.recon.reconciliation.entity.ReconRunEntity;
import com.recon.reconciliation.service.ReconciliationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reconciliations")
@CrossOrigin(origins = "*")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/trigger")
    public ResponseEntity<ReconRunEntity> triggerReconciliation(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate reconDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(reconciliationService.runReconciliation(reconDate));
    }

    @GetMapping
    public ResponseEntity<List<ReconRunEntity>> getAllRuns() {
        return ResponseEntity.ok(reconciliationService.getAllRuns());
    }

    @GetMapping("/{runId}")
    public ResponseEntity<ReconRunEntity> getRunDetails(@PathVariable String runId) {
        return reconciliationService.getRunById(runId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
