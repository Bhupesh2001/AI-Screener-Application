package com.stockresearch.controller;

import com.stockresearch.dto.DashboardDto;
import com.stockresearch.service.DashboardService;
import com.stockresearch.service.SettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final SettingsService settingsService;

    public DashboardController(DashboardService dashboardService, SettingsService settingsService) {
        this.dashboardService = dashboardService;
        this.settingsService = settingsService;
    }

    @GetMapping
    public DashboardDto getDashboard() {
        int threshold = settingsService.getSettings().getMinScoreThreshold();
        return dashboardService.getDashboard(threshold);
    }
}
