package com.searchSystem.service;

import com.searchSystem.model.Document;
import com.searchSystem.service.indexing.IndexingWorkerService;

import org.springframework.stereotype.Service;

@Service
public class IngestionService {

    private final IndexingWorkerService indexingWorkerService;

    public IngestionService(IndexingWorkerService indexingWorkerService) {
        this.indexingWorkerService = indexingWorkerService;
    }

    public void ingest(Document document) {
        indexingWorkerService.submit(document);
    }
}
