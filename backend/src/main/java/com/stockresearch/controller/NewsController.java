package com.stockresearch.controller;

import com.stockresearch.dto.NewsDto;
import com.stockresearch.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    public List<NewsDto> getRecentNews(@RequestParam(defaultValue = "50") int limit) {
        return newsService.getRecentNews(limit);
    }

    @GetMapping("/company/{companyId}")
    public List<NewsDto> getNewsForCompany(@PathVariable Long companyId) {
        return newsService.getNewsForCompany(companyId);
    }

    @PostMapping("/refresh/{companyId}")
    public List<NewsDto> refreshNewsForCompany(@PathVariable Long companyId) {
        return newsService.refreshNewsForCompany(companyId);
    }
}