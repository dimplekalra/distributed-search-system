package com.searchSystem.index;

import com.searchSystem.model.SearchResult;
import com.searchSystem.model.Document;
import com.searchSystem.tokenizer.Tokenizer;
import com.searchSystem.autoComplete.Trie;
import com.searchSystem.model.SnapshotData;
import com.searchSystem.service.search.DocumentStatisticsService;
import com.searchSystem.service.search.RankingService;
import com.searchSystem.service.search.SegmentSearchService;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.javapoet.ClassName;

import java.util.*;

public class Shard {
    private static final Logger logger = LoggerFactory.getLogger(Shard.class);
    private final List<IndexSegment> segments = new ArrayList<>();
    private IndexSegment activeSegment;
    private static final int SEGMENT_MAX_DOCS = 100;
    private final Tokenizer tokenizer = new Tokenizer();
    private final Trie trie;
    private final SegmentSearchService segmentSearchService;
    private final DocumentStatisticsService statsService;
    private final RankingService rankingService;
    private boolean available = true;
    private static final int MERGE_THRESHOLD = 3;
    private transient ReadWriteLock lock = new ReentrantReadWriteLock();

    public Shard(Trie trie, DocumentStatisticsService statsService, SegmentSearchService segmentSearchService,
            RankingService rankingService) {
        this.trie = trie;
        this.statsService = statsService;
        this.rankingService = rankingService;
        this.segmentSearchService = segmentSearchService;
        this.activeSegment = new IndexSegment(statsService, rankingService);
    }

    public synchronized void addDocument(Document document) {

        initializeTransientFields();

        activeSegment.addDocument(document);

        List<String> tokens = tokenizer.tokenize(document.getContent());

        for (String token : tokens) {
            trie.insert(token);
        }

        statsService.recordDocument(
                document.getId(),
                tokens.size());

        if (activeSegment.getDocumentCount() >= SEGMENT_MAX_DOCS) {
            rotateSegment();
        }
    }

    public synchronized List<IndexSegment> getImmutableSegments() {
        return new ArrayList<>(segments);
    }

    public List<SearchResult> search(List<String> terms, boolean phraseQuery) {
        initializeTransientFields();
        lock.readLock().lock();

        try {
            if (!available) {
                throw new RuntimeException(
                        "Shard is unavailable");
            }

            List<IndexSegment> searchableSegments = new ArrayList<>(segments);

            searchableSegments.add(activeSegment);

            return segmentSearchService.searchSegments(
                    searchableSegments,
                    terms, phraseQuery);

        } finally {
            lock.readLock().unlock();
        }

    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }

    public SnapshotData exportSnapshot() {

        List<IndexSegment> allSegments = new ArrayList<>(segments);

        allSegments.add(activeSegment);

        return new SnapshotData(allSegments);
    }

    public void loadSnapshot(SnapshotData snapshotData) {

        this.segments.clear();

        this.segments.addAll(
                snapshotData.getSegments());

        for (IndexSegment segment : segments) {

            segment.initializeServices(
                    statsService,
                    rankingService);
        }

        this.activeSegment = new IndexSegment(statsService, rankingService);

        logger.info("Shard snapshot loaded successfully");
    }

    private synchronized void mergeSegments() {

        logger.info("Starting segment merge...");

        IndexSegment mergedSegment = new IndexSegment(statsService, rankingService);

        List<IndexSegment> segmentsToMerge = new ArrayList<>(segments);

        for (IndexSegment segment : segmentsToMerge) {

            mergedSegment.merge(segment);
        }

        mergedSegment.seal();

        segments.clear();

        segments.add(mergedSegment);
        logger.info(
                "Segment merge completed. Active segments: ", segments.size());
    }

    private synchronized void rotateSegment() {
        activeSegment.seal();
        segments.add(activeSegment);
        activeSegment = new IndexSegment(statsService, rankingService);

        logger.info(
                "Segment rotated. Total segments: ", segments.size());

        if (segments.size() >= MERGE_THRESHOLD) {
            mergeSegments();
        }
    }

    public synchronized void replaceSegment(List<IndexSegment> oldSegments, IndexSegment mergedSegment) {

        initializeTransientFields();

        lock.writeLock().lock();

        try {
            segments.removeAll(oldSegments);

            mergedSegment.seal();

            segments.add(mergedSegment);

            logger.info(
                    "Merged ", oldSegments.size(), " segments. Remaining segments: ", segments.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void incrementPopularity(String documentId) {

        lock.writeLock().lock();

        try {

            activeSegment.incrementPopularity(documentId);

            for (IndexSegment segment : segments) {
                segment.incrementPopularity(documentId);
            }

        } finally {
            lock.writeLock().unlock();
        }
    }

    private void initializeTransientFields() {

        if (lock == null) {
            lock = new ReentrantReadWriteLock();
        }
    }

}
