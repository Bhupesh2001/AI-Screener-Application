package com.stockresearch.controller;

import com.stockresearch.dto.EventDto;
import com.stockresearch.service.EventCenterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventCenterService eventCenterService;

    public EventController(EventCenterService eventCenterService) {
        this.eventCenterService = eventCenterService;
    }

    @GetMapping
    public List<EventDto> getRecentEvents(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "50") int limit
    ) {
        if (type != null && !type.isBlank()) {
            return eventCenterService.getEventsByType(type, limit);
        }
        return eventCenterService.getRecentEvents(limit);
    }

    @GetMapping("/company/{companyId}")
    public List<EventDto> getEventsForCompany(@PathVariable Long companyId) {
        return eventCenterService.getEventsForCompany(companyId);
    }
}
