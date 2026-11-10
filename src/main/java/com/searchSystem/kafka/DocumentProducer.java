package com.searchSystem.kafka;

import com.searchSystem.model.Document;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class DocumentProducer {
    private static final String Topic = "document-ingestion";

    private final KafkaTemplate<String, Document> kafkaTemplate;

    public DocumentProducer(KafkaTemplate<String, Document> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(Document document) {
        kafkaTemplate.send(Topic, document);
    }
}
