package com.searchSystem.service.indexing;

import com.searchSystem.index.IndexSegment;
import com.searchSystem.index.Shard;
import com.searchSystem.repository.DocumentRepository;
import com.searchSystem.service.ShardRouter;
import com.searchSystem.service.search.DocumentStatisticsService;
import com.searchSystem.service.search.RankingService;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SegmentMergeService {
    private static final int MERGE_THRESHOLD = 5;
    private final DocumentStatisticsService statsService;
    private final ShardRouter shardRouter;
    private final RankingService rankingService;

    public SegmentMergeService(ShardRouter shardRouter, DocumentStatisticsService statsService,
            RankingService rankingService) {
        this.shardRouter = shardRouter;
        this.statsService = statsService;
        this.rankingService = rankingService;
    }

    @PostConstruct
    public void startMergeWorker() {
        Thread mergeThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000);

                    performMergeCycle();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mergeThread.setDaemon(true);
        mergeThread.setName("segment-merge-thread");

        mergeThread.start();
    }

    private void performMergeCycle() {
        for (Shard shard : shardRouter.getAllShards()) {
            List<IndexSegment> immutableSegments = shard.getImmutableSegments();

            if (immutableSegments.size() < MERGE_THRESHOLD) {
                continue;
            }

            System.out.println(
                    "Starting segment merge. Segment count: "
                            + immutableSegments.size());

            IndexSegment mergedSegment = new IndexSegment(statsService, rankingService);

            for (IndexSegment segment : immutableSegments) {
                mergedSegment.merge(segment);
            }

            shard.replaceSegment(immutableSegments, mergedSegment);

            System.out.println(
                    "Segment merge completed");
        }
    }
}
