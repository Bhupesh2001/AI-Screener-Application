package com.stockresearch.service.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Primary
@Component
@RequiredArgsConstructor
public class IndianApiAnnouncementSource implements AnnouncementSource {

    private final IndianApiClient apiClient;

    @Override
    public List<RawAnnouncement> fetchRecentAnnouncements(String companySymbol) {
        JsonNode root = apiClient.getStockData(companySymbol);
        JsonNode actions = root.path("corporate_actions");
        List<RawAnnouncement> list = new ArrayList<>();
        if (actions.isArray()) {
            for (JsonNode action : actions) {
                String title = action.path("title").asText();
                String desc = action.path("description").asText();
                String dateStr = action.path("date").asText();
                LocalDateTime date = LocalDateTime.parse(dateStr); // adjust format
                list.add(new RawAnnouncement(title, desc, date, null, "NSE", null));
            }
        }
        return list;
    }
}