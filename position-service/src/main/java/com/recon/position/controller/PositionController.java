package com.recon.position.controller;

import com.recon.position.entity.PositionEntity;
import com.recon.position.service.PositionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/positions")
@CrossOrigin(origins = "*")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping
    public ResponseEntity<List<PositionEntity>> getAllPositions() {
        return ResponseEntity.ok(positionService.getAllPositions());
    }
}
