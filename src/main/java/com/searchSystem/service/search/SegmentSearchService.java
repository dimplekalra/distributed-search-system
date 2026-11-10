package com.searchSystem.service.search;

import com.searchSystem.index.IndexSegment;
import com.searchSystem.model.SearchResult;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
public class SegmentSearchService {
    private final ExecutorService executorService = Executors
            .newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public List<SearchResult> searchSegments(List<IndexSegment> segments, List<String> terms, boolean phraseQuery) {
        List<CompletableFuture<List<SearchResult>>> futures = new ArrayList<>();

        for (IndexSegment segment : segments) {

            CompletableFuture<List<SearchResult>> future = CompletableFuture.supplyAsync(() -> {

                try {

                    if (phraseQuery) {
                        return segment.phraseSearch(terms);
                    } else {
                        return segment.search(terms);
                    }
                } catch (Exception e) {

                    System.err.println(
                            "Segment search failed: "
                                    + e.getMessage());

                    return Collections.emptyList();
                }

            }, executorService);

            futures.add(future);
        }

        Map<String, Double> aggregatedScores = new HashMap<>();

        for (CompletableFuture<List<SearchResult>> future : futures) {

            try {

                List<SearchResult> results = future.get(200, TimeUnit.MILLISECONDS);

                for (SearchResult result : results) {

                    aggregatedScores.put(
                            result.getDocumentId(),
                            aggregatedScores.getOrDefault(
                                    result.getDocumentId(),
                                    0.0) + result.getScore());
                }

            } catch (Exception e) {

                System.err.println(
                        "Segment search timeout/failure");
            }
        }

        List<SearchResult> finalResults = new ArrayList<>();

        for (Map.Entry<String, Double> entry : aggregatedScores.entrySet()) {

            finalResults.add(
                    new SearchResult(
                            entry.getKey(),
                            entry.getValue()));
        }

        return finalResults;
    }
}
