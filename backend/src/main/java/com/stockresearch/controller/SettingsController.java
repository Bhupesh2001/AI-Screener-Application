package com.stockresearch.controller;

import com.stockresearch.dto.SettingsDto;
import com.stockresearch.service.SettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public SettingsDto getSettings() {
        return settingsService.getSettingsDto();
    }

    @PutMapping
    public SettingsDto updateSettings(@RequestBody SettingsDto dto) {
        return settingsService.updateSettings(dto);
    }
}
