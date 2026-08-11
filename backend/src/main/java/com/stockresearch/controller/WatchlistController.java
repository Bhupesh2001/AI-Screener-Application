package com.stockresearch.controller;

import com.stockresearch.dto.CompanySummaryDto;
import com.stockresearch.service.WatchlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public List<CompanySummaryDto> getWatchlist() {
        return watchlistService.getWatchlist();
    }

    @PostMapping("/{companyId}")
    public ResponseEntity<Void> addToWatchlist(@PathVariable Long companyId, @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.get("note") : null;
        return watchlistService.addToWatchlist(companyId, note).isPresent()
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{companyId}")
    public ResponseEntity<Void> removeFromWatchlist(@PathVariable Long companyId) {
        watchlistService.removeFromWatchlist(companyId);
        return ResponseEntity.noContent().build();
    }
}
