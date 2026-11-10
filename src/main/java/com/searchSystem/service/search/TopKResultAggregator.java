package com.searchSystem.service.search;

import com.searchSystem.model.SearchResult;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TopKResultAggregator {
    public List<SearchResult> aggregate(
            List<List<SearchResult>> shardResults,
            int page,
            int size) {

        List<SearchResult> mergedResults = new ArrayList<>();

        for (List<SearchResult> results : shardResults) {
            mergedResults.addAll(results);
        }

        mergedResults.sort((a, b) -> Double.compare(
                b.getScore(),
                a.getScore()));

        int start = page * size;

        if (start >= mergedResults.size()) {
            return Collections.emptyList();
        }

        int end = Math.min(
                start + size,
                mergedResults.size());

        return mergedResults.subList(start, end);
    }
}
