package com.searchSystem.service;

import com.searchSystem.index.Shard;
import com.searchSystem.model.Document;
import com.searchSystem.repository.DocumentRepository;
import com.searchSystem.service.search.DocumentStatisticsService;
import com.searchSystem.service.search.RankingService;
import com.searchSystem.service.search.SegmentSearchService;
import com.searchSystem.autoComplete.Trie;

import java.util.*;

public class ShardRouter {
    private final List<Shard> shards;
    private final Trie trie;
    private final DocumentStatisticsService statsService;
    private final SegmentSearchService segmentSearchService;
    private final RankingService rankingService;

    public ShardRouter(Trie trie, DocumentStatisticsService statsService, SegmentSearchService segmentSearchService,
            RankingService rankingService) {
        this.shards = new ArrayList<>();
        this.trie = trie;
        this.statsService = statsService;
        this.segmentSearchService = segmentSearchService;
        this.rankingService = rankingService;

        int numShards = 4; // For simplicity, we use a fixed number of shards. In production, this could be
                           // dynamic.

        for (int i = 0; i < numShards; i++) {
            shards.add(new Shard(trie, statsService, segmentSearchService, rankingService));
        }
    }

    private int getShardId(String documentId) {
        return Math.abs(documentId.hashCode()) % shards.size();
    }

    public void routeDocument(Document document) {
        int shardId = getShardId(document.getId());
        shards.get(shardId).addDocument(document);
    }

    public List<Shard> getAllShards() {
        return shards;
    }
}
