package com.searchSystem.index;

import com.searchSystem.model.Document;
import com.searchSystem.model.DocumentMetadata;
import com.searchSystem.model.Posting;
import com.searchSystem.tokenizer.Tokenizer;
import com.searchSystem.model.SearchResult;
import com.searchSystem.service.search.DocumentStatisticsService;
import com.searchSystem.service.search.RankingService;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;

public class IndexSegment implements Serializable {
    private final String segmentId;
    private final long createdAt;
    private boolean sealed;
    private int documentCount;
    private final InvertedIndex invertedIndex = new InvertedIndex();
    private final Map<String, DocumentMetadata> documentMetadataMap = new HashMap<>();
    private transient Tokenizer tokenizer = new Tokenizer();
    private transient DocumentStatisticsService statsService;
    private transient RankingService rankingService;

    public IndexSegment(DocumentStatisticsService statsService, RankingService rankingService) {
        this.segmentId = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.sealed = false;
        this.documentCount = 0;
        this.statsService = statsService;
        this.rankingService = rankingService;
    }

    public void addDocument(Document document) {

        initializeTransientFields();

        if (sealed) {
            throw new RuntimeException("Cannot write to sealed segment");
        }

        documentCount++;

        List<String> terms = tokenizer.tokenize(
                document.getContent());

        documentMetadataMap.put(
                document.getId(),
                new DocumentMetadata(
                        terms.size(),
                        document.getPopularityScore(),
                        System.currentTimeMillis()));

        for (int position = 0; position < terms.size(); position++) {

            invertedIndex.add(
                    terms.get(position),
                    document.getId(),
                    position);
        }
    }

    public List<SearchResult> search(List<String> terms) {
        initializeTransientFields();

        Map<String, Double> scores = new HashMap<>();

        Map<String, Integer> matchedTermsCount = new HashMap<>();

        int totalDocs = (int) Math.max(
                statsService.getTotalDocuments(),
                1);

        double avgDocLength = Math.max(
                statsService.getAverageDocumentLength(),
                1.0);

        final double K1 = 1.2;
        final double B = 0.75;

        for (String term : terms) {

            List<Posting> postings = invertedIndex.getPostings(term);

            int docFreq = invertedIndex.getDocumentFrequency(term);

            if (docFreq == 0) {
                continue;
            }

            double idf = Math.log(
                    ((totalDocs - docFreq + 0.5)
                            / (docFreq + 0.5)) + 1.0);

            for (Posting posting : postings) {

                double tf = posting.getTermFrequency();

                DocumentMetadata metadata = documentMetadataMap.get(
                        posting.getDocumentId());

                if (metadata == null) {
                    continue;
                }

                int docLength = metadata.getDocumentLength();

                double numerator = tf * (K1 + 1);

                double denominator = tf + K1 * (1 - B +
                        B * ((double) docLength
                                / avgDocLength));

                double bm25Score = idf * (numerator / denominator);

                scores.put(
                        posting.getDocumentId(),
                        scores.getOrDefault(
                                posting.getDocumentId(),
                                0.0) + bm25Score);
                matchedTermsCount.put(
                        posting.getDocumentId(),
                        matchedTermsCount.getOrDefault(
                                posting.getDocumentId(),
                                0) + 1);
            }
        }

        List<SearchResult> results = new ArrayList<>();

        for (Map.Entry<String, Double> entry : scores.entrySet()) {

            String documentId = entry.getKey();

            double bm25Score = entry.getValue();

            int matchedTerms = matchedTermsCount.getOrDefault(
                    documentId,
                    0);

            DocumentMetadata metadata = documentMetadataMap.get(documentId);

            if (metadata == null) {
                continue;
            }

            long popularity = metadata.getPopularityScore();

            double finalScore = rankingService.calculateFinalScore(
                    bm25Score,
                    matchedTerms,
                    terms.size(),
                    false,
                    popularity);

            results.add(
                    new SearchResult(
                            documentId,
                            finalScore));
        }

        results.sort((a, b) -> Double.compare(
                b.getScore(),
                a.getScore()));

        return results;
    }

    public List<SearchResult> phraseSearch(
            List<String> terms) {

        if (terms.isEmpty()) {
            return Collections.emptyList();
        }

        List<Posting> firstPostings = invertedIndex.getPostings(
                terms.get(0));

        Map<String, Double> matches = new HashMap<>();

        for (Posting posting : firstPostings) {

            String docId = posting.getDocumentId();

            List<Integer> candidatePositions = posting.getPositions();

            boolean phraseMatch = true;

            for (int i = 1; i < terms.size(); i++) {

                List<Posting> nextPostings = invertedIndex.getPostings(
                        terms.get(i));

                Posting matchingPosting = null;

                for (Posting p : nextPostings) {

                    if (p.getDocumentId()
                            .equals(docId)) {

                        matchingPosting = p;
                        break;
                    }
                }

                if (matchingPosting == null) {
                    phraseMatch = false;
                    break;
                }

                Set<Integer> nextPositions = new HashSet<>(matchingPosting.getPositions());

                boolean foundAdjacent = false;

                for (Integer pos : candidatePositions) {

                    if (nextPositions.contains(pos + i)) {
                        foundAdjacent = true;
                        break;
                    }
                }

                if (!foundAdjacent) {
                    phraseMatch = false;
                    break;
                }
            }

            if (phraseMatch) {
                DocumentMetadata metadata = documentMetadataMap.get(docId);

                if (metadata == null) {
                    continue;
                }

                long popularity = metadata.getPopularityScore();

                double boostedScore = rankingService.calculateFinalScore(
                        1.0,
                        terms.size(),
                        terms.size(),
                        true,
                        popularity);

                matches.put(docId, boostedScore);
            }
        }

        List<SearchResult> results = new ArrayList<>();

        for (Map.Entry<String, Double> entry : matches.entrySet()) {

            results.add(
                    new SearchResult(
                            entry.getKey(),
                            entry.getValue()));
        }

        return results;
    }

    public List<Posting> getPostings(String term) {
        return invertedIndex.getPostings(term);
    }

    public int getDocumentFrequency(String term) {
        return invertedIndex.getDocumentFrequency(term);
    }

    public void merge(IndexSegment otherSegment) {

        if (sealed) {
            throw new RuntimeException(
                    "Cannot merge into sealed segment");
        }

        this.invertedIndex.merge(
                otherSegment.getInvertedIndex());

        this.documentCount += otherSegment.getDocumentCount();
    }

    public void seal() {
        this.sealed = true;
    }

    public String getSegmentId() {
        return segmentId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isSealed() {
        return sealed;
    }

    public int getDocumentCount() {
        return documentCount;
    }

    public InvertedIndex getInvertedIndex() {
        return invertedIndex;
    }

    public void setStatsService(
            DocumentStatisticsService statsService) {

        this.statsService = statsService;
    }

    public void initializeServices(
            DocumentStatisticsService statsService,
            RankingService rankingService) {

        this.statsService = statsService;
        this.rankingService = rankingService;
    }

    public DocumentMetadata getDocumentMetadata(
            String documentId) {

        return documentMetadataMap.get(documentId);
    }

    public void incrementPopularity(String documentId) {

        DocumentMetadata metadata = documentMetadataMap.get(documentId);

        if (metadata != null) {
            metadata.incrementPopularity();
        }
    }

    private void initializeTransientFields() {

        if (tokenizer == null) {
            tokenizer = new Tokenizer();
        }

        if (statsService == null) {
            throw new RuntimeException(
                    "DocumentStatisticsService not initialized");
        }
    }
}
