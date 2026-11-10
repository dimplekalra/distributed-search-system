package com.searchSystem.service.indexing;

import com.searchSystem.service.QueryService;
import com.searchSystem.service.ShardRouter;
import com.searchSystem.model.Document;
import com.searchSystem.model.IndexStatus;
import com.searchSystem.repository.DocumentRepository;
import com.searchSystem.cache.RedisCacheService;
import com.searchSystem.service.persistence.IndexSnapshotService;
import com.searchSystem.service.wal.WriteAheadLogService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
public class BatchIndexProcessor {

    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);

    private final ShardRouter shardRouter;
    private final DocumentRepository documentRepository;
    private final RedisCacheService cacheService;
    private final IndexSnapshotService snapshotService;
    private final WriteAheadLogService walService;

    public BatchIndexProcessor(ShardRouter shardRouter, DocumentRepository documentRepository,
            RedisCacheService cacheService, IndexSnapshotService snapshotService, WriteAheadLogService walService) {
        this.shardRouter = shardRouter;
        this.documentRepository = documentRepository;
        this.cacheService = cacheService;
        this.snapshotService = snapshotService;
        this.walService = walService;
    }

    @Transactional
    public void processBatch(List<Document> documents) {

        logger.info(
                "Processing batch of size: " + documents.size());

        List<Document> successfullyIndexed = new ArrayList<>();

        try {

            for (Document doc : documents) {
                walService.append(doc); // Append to WAL before processing
                doc.setIndexStatus(IndexStatus.PENDING);
            }

            documentRepository.saveAll(documents);

            for (Document doc : documents) {
                try {

                    shardRouter.routeDocument(doc);

                    doc.setIndexStatus(IndexStatus.INDEXED);

                    doc.setIndexedAt(LocalDateTime.now());

                    successfullyIndexed.add(doc);

                } catch (Exception e) {
                    doc.setIndexStatus(IndexStatus.FAILED);

                    documentRepository.save(doc);

                    logger.error(
                            "Failed indexing document: "
                                    + doc.getId(),
                            e);

                }
            }

            documentRepository.saveAll(documents);

            cacheService.clearCache(); // Clear cache after batch indexing to ensure fresh results

            boolean snapshotSaved = snapshotService.saveAllSnapshots(); // Save snapshots after batch indexing

            if (snapshotSaved) {
                walService.clear(); // Clear WAL after successful snapshot save
                logger.info(
                        "Snapshots saved successfully after batch indexing.");
            } else {
                logger.info(
                        "Failed to save snapshots after batch indexing.");
            }
            walService.clear();

            logger.info(
                    "Successfully indexed documents: "
                            + successfullyIndexed.size());

        } catch (Exception e) {
            logger.error(
                    "Batch indexing failed: ", e);

            e.printStackTrace();
        }
    }
}
