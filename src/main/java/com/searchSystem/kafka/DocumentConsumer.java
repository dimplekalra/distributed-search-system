package com.searchSystem.kafka;

import com.searchSystem.model.Document;
import com.searchSystem.service.IngestionService;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DocumentConsumer {
    private static final Logger logger = LoggerFactory.getLogger(DocumentConsumer.class);
    private final IngestionService ingestionService;

    public DocumentConsumer(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @KafkaListener(topics = "document-ingestion", groupId = "search-index-group")
    public void consume(Document document) {
        logger.info(
                "Received document from Kafka: "
                        + document.getId());

        ingestionService.ingest(document);
    }
}
