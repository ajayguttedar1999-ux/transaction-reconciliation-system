package com.recon.stale.controller;

import com.recon.stale.entity.ReconRunEntity;
import com.recon.stale.service.StaleDetectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stale-runs")
@CrossOrigin(origins = "*")
public class StaleRunController {

    private final StaleDetectionService staleDetectionService;

    public StaleRunController(StaleDetectionService staleDetectionService) {
        this.staleDetectionService = staleDetectionService;
    }

    @GetMapping
    public ResponseEntity<List<ReconRunEntity>> getStaleRuns() {
        return ResponseEntity.ok(staleDetectionService.getStaleRuns());
    }
}
