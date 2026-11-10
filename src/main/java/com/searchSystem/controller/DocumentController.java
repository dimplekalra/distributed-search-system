package com.searchSystem.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.searchSystem.model.Document;
import com.searchSystem.service.IngestionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.searchSystem.kafka.DocumentProducer;

@RestController
@RequestMapping("/documents")
public class DocumentController {
    private final IngestionService ingestionService;
    private final DocumentProducer producer;

    public DocumentController(IngestionService ingestionService, DocumentProducer documentProducer) {
        this.ingestionService = ingestionService;
        this.producer = documentProducer;
    }

    @PostMapping
    public String addDocument(@RequestBody Document document) {
        producer.publish(document);
        return "Document added successfully";
    }

}
