package com.searchSystem.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QueryTracker {
    private final Map<String, Integer> queryCount = new ConcurrentHashMap<>();

    public void record(String query) {
        queryCount.merge(query.toLowerCase(), 1, Integer::sum);
    }

    public int getFrequency(String query) {
        return queryCount.getOrDefault(query.toLowerCase(), 0);
    }

    public List<String> topQueries() {
        return queryCount.entrySet()
                .stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .limit(10)
                .toList();
    }
}
