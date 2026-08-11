package com.stockresearch.controller;

import com.stockresearch.dto.SectorSummaryDto;
import com.stockresearch.service.SectorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sectors")
public class SectorController {

    private final SectorService sectorService;

    public SectorController(SectorService sectorService) {
        this.sectorService = sectorService;
    }

    @GetMapping
    public List<SectorSummaryDto> getAllSectors() {
        return sectorService.getAllSectorSummaries();
    }

    @GetMapping("/{sector}")
    public ResponseEntity<SectorSummaryDto> getSector(@PathVariable String sector) {
        if (!SectorService.TRACKED_SECTORS.stream().anyMatch(s -> s.equalsIgnoreCase(sector))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(sectorService.getSectorSummary(sector));
    }
}
