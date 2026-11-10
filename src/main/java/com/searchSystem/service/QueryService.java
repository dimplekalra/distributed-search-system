package com.searchSystem.service;

import com.searchSystem.cache.RedisCacheService;
import com.searchSystem.model.SearchResponse;
import com.searchSystem.model.SearchResult;
import com.searchSystem.repository.DocumentRepository;
import com.searchSystem.tokenizer.Tokenizer;
import com.searchSystem.service.search.TopKResultAggregator;
import com.searchSystem.service.search.QueryCoordinatorService;
import com.searchSystem.service.search.SnippetService;
import com.searchSystem.service.search.booleanquery.BooleanQueryParser;
import com.searchSystem.service.search.booleanquery.BooleanQueryProcessor;
import com.searchSystem.service.search.booleanquery.BooleanToken;
import com.searchSystem.service.search.fuzzy.FuzzySearchService;
import com.searchSystem.model.Document;

import java.util.*;

import org.springframework.stereotype.Service;

@Service
public class QueryService {
    private final Tokenizer tokenizer = new Tokenizer();
    private final QueryTracker tracker;
    private final RedisCacheService redisCache;
    private final QueryCoordinatorService queryCoordinatorService;
    private final DocumentRepository documentRepository;
    private final BooleanQueryParser booleanQueryParser;
    private final BooleanQueryProcessor booleanQueryProcessor;
    private final FuzzySearchService fuzzySearchService;
    private final SnippetService snippetService;

    public QueryService(QueryTracker queryTracker, RedisCacheService redisCache,
            QueryCoordinatorService queryCoordinatorService, DocumentRepository documentRepository,
            BooleanQueryParser booleanQueryParser, BooleanQueryProcessor booleanQueryProcessor,
            FuzzySearchService fuzzySearchService, SnippetService snippetService) {

        this.tracker = queryTracker;
        this.redisCache = redisCache;
        this.queryCoordinatorService = queryCoordinatorService;
        this.documentRepository = documentRepository;
        this.booleanQueryParser = booleanQueryParser;
        this.booleanQueryProcessor = booleanQueryProcessor;
        this.fuzzySearchService = fuzzySearchService;
        this.snippetService = snippetService;
    }

    public List<SearchResponse> search(String query, int page, int size) {
        tracker.record(query);

        boolean isPhraseQuery = query.startsWith("\"")
                && query.endsWith("\"");
        String cleanedQuery = query.replace("\"", "");

        String normalizedQuery = cleanedQuery
                .trim()
                .toLowerCase();

        List<SearchResponse> cachedResults = redisCache.get(normalizedQuery);

        if (cachedResults != null) {
            return cachedResults;
        }

        queryCoordinatorService.getTrie().insert(normalizedQuery);

        List<SearchResponse> enrichedResults = new ArrayList<>();

        boolean isBooleanQuery = cleanedQuery.contains(" AND ")
                || cleanedQuery.contains(" OR ")
                || cleanedQuery.contains(" NOT ");

        List<String> terms = tokenizer.tokenize(normalizedQuery);

        if (terms.isEmpty()) {
            return Collections.emptyList();
        }

        if (isBooleanQuery) {

            List<BooleanToken> tokens = booleanQueryParser.parse(
                    cleanedQuery);

            List<SearchResult> results = booleanQueryProcessor.execute(
                    tokens, page, size);

            for (SearchResult result : results) {

                documentRepository.findById(
                        result.getDocumentId())
                        .ifPresent(document -> {

                            document.incrementPopularity();

                            queryCoordinatorService.incrementPopularity(
                                    document.getId());

                            String snippet = snippetService.generateSnippet(
                                    document.getContent(),
                                    terms);

                            enrichedResults.add(
                                    new SearchResponse(
                                            document.getId(),
                                            result.getScore(),
                                            document.getContent(),
                                            snippet));
                        });
            }

            return enrichedResults;
        }

        List<SearchResult> finalResults = queryCoordinatorService.executeDistributedQuery(terms, page, size,
                isPhraseQuery);

        if (finalResults.isEmpty()
                && terms.size() == 1) {

            String corrected = fuzzySearchService
                    .findClosestTerm(
                            terms.get(0));

            if (corrected != null) {

                finalResults = queryCoordinatorService
                        .executeDistributedQuery(
                                List.of(corrected),
                                page,
                                size,
                                false);

                System.out.println(
                        "Did you mean: "
                                + corrected);
            }
        }

        Map<String, SearchResult> scoreMap = new HashMap<>();

        List<String> ids = new ArrayList<>();

        for (SearchResult result : finalResults) {
            ids.add(result.getDocumentId());
            scoreMap.put(result.getDocumentId(), result);
        }

        Iterable<Document> documents = documentRepository.findAllById(ids);

        for (Document document : documents) {

            document.incrementPopularity();

            queryCoordinatorService.incrementPopularity(
                    document.getId());

            SearchResult result = scoreMap.get(document.getId());

            String snippet = snippetService.generateSnippet(
                    document.getContent(),
                    terms);

            enrichedResults.add(
                    new SearchResponse(
                            document.getId(),
                            result.getScore(),
                            document.getContent(),
                            snippet));
        }

        redisCache.put(normalizedQuery, enrichedResults);

        return enrichedResults;
    }

    public List<String> topQueries() {
        return tracker.topQueries();
    }

    public void clearCache() {
        redisCache.clearCache();
    }
}
