package com.searchSystem.service.search;

import com.searchSystem.autoComplete.Trie;
import com.searchSystem.index.Shard;
import com.searchSystem.service.ShardRouter;

import com.searchSystem.model.SearchResult;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Service
public class QueryCoordinatorService {
    private final ShardRouter shardRouter;
    private final TopKResultAggregator aggregator;
    private final Trie trie;

    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());

    public QueryCoordinatorService(ShardRouter shardRouter, TopKResultAggregator aggregator, Trie trie) {
        this.shardRouter = shardRouter;
        this.aggregator = aggregator;
        this.trie = trie;
    }

    public List<SearchResult> executeDistributedQuery(List<String> terms, int page, int size, boolean phraseQuery) {
        List<CompletableFuture<List<SearchResult>>> futures = new ArrayList<>();

        for (Shard shard : shardRouter.getAllShards()) {
            CompletableFuture<List<SearchResult>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return shard.search(terms, phraseQuery);
                } catch (Exception e) {
                    System.out.println(
                            "Shard query failed: "
                                    + e.getMessage());

                    return Collections.emptyList();
                }
            }, executorService);

            futures.add(future);
        }

        List<List<SearchResult>> shardResults = new ArrayList<>();

        for (CompletableFuture<List<SearchResult>> future : futures) {
            try {
                List<SearchResult> results = future.get(200, TimeUnit.MILLISECONDS);

                shardResults.add(results);
            } catch (TimeoutException e) {
                System.out.println(
                        "Shard query timed out: "
                                + e.getMessage());

                shardResults.add(Collections.emptyList());
            } catch (Exception e) {
                System.out.println(
                        "Failed getting shard result: "
                                + e.getMessage());

                shardResults.add(Collections.emptyList());
            }
        }

        return aggregator.aggregate(shardResults, page, size);
    }

    public void incrementPopularity(String documentId) {

        for (Shard shard : shardRouter.getAllShards()) {
            shard.incrementPopularity(documentId);
        }
    }

    public Trie getTrie() {
        return trie;
    }
}
