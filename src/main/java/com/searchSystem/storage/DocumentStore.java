package com.searchSystem.storage;

import java.util.*;

import com.searchSystem.model.Document;

public class DocumentStore {
    private Map<String, Document> store = new HashMap<>();
    
    public void save(Document document) {
        store.put(document.getId(), document);
    }

    public Document get(String documentId) {
        return store.get(documentId);
    }

    public int getTotalDocuments() {
        return store.size();
    }
}
