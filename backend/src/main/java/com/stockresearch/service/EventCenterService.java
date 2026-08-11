package com.stockresearch.service;

import com.stockresearch.domain.Event;
import com.stockresearch.dto.EventDto;
import com.stockresearch.repository.EventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/** Backs the Event Center module: all corporate events, newest first, optionally filtered by type. */
@Service
public class EventCenterService {

    private final EventRepository eventRepository;

    public EventCenterService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<EventDto> getRecentEvents(int limit) {
        return eventRepository.findAllByOrderByEventDateDesc(PageRequest.of(0, limit)).stream()
                .map(this::toDto)
                .toList();
    }

    public List<EventDto> getEventsByType(String type, int limit) {
        Event.EventType eventType = Event.EventType.valueOf(type.toUpperCase());
        return eventRepository.findByTypeOrderByEventDateDesc(eventType, PageRequest.of(0, limit)).stream()
                .map(this::toDto)
                .toList();
    }

    public List<EventDto> getEventsForCompany(Long companyId) {
        return eventRepository.findByCompanyIdOrderByEventDateDesc(companyId).stream()
                .map(this::toDto)
                .toList();
    }

    private EventDto toDto(Event e) {
        return EventDto.builder()
                .id(e.getId())
                .companyId(e.getCompany().getId())
                .companySymbol(e.getCompany().getSymbol())
                .companyName(e.getCompany().getName())
                .type(e.getType().name())
                .title(e.getTitle())
                .description(e.getDescription())
                .valueCr(e.getValueCr())
                .eventDate(e.getEventDate())
                .sourceUrl(e.getSourceUrl())
                .source(e.getSource())
                .scoreImpact(e.getScoreImpact())
                .build();
    }
}
