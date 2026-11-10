package com.searchSystem.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.searchSystem.model.SearchResponse;
import com.searchSystem.service.QueryService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/search")
public class SearchController {
    private final QueryService queryService;

    public SearchController(QueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public List<SearchResponse> search(@RequestParam("q") String query, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return queryService.search(query, page, size);
    }

    @GetMapping("/top-queries")
    public List<String> topQueries() {
        return queryService.topQueries();
    }
}
