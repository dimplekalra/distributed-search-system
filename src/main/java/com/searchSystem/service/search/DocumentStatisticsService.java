package com.searchSystem.service.search;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DocumentStatisticsService {
    private final Map<String, Integer> documentLengths = new ConcurrentHashMap<>();

    private final AtomicLong totalDocuments = new AtomicLong(0);

    private final AtomicLong totalTokens = new AtomicLong(0);

    public void recordDocument(
            String documentId,
            int tokenCount) {

        Integer existing = documentLengths.put(documentId, tokenCount);

        if (existing == null) {
            totalDocuments.incrementAndGet();
            totalTokens.addAndGet(tokenCount);
        }
    }

    public int getDocumentLength(String documentId) {
        return documentLengths.getOrDefault(documentId, 0);
    }

    public long getTotalDocuments() {
        return totalDocuments.get();
    }

    public double getAverageDocumentLength() {

        if (totalDocuments.get() == 0) {
            return 0.0;
        }

        return (double) totalTokens.get()
                / totalDocuments.get();
    }
}
