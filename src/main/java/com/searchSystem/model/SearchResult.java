package com.searchSystem.model;

import java.io.Serializable;

public class SearchResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String documentId;
    private double score;

    public SearchResult(String documentId, double score) {
        this.documentId = documentId;
        this.score = score;
    }

    public String getDocumentId() {
        return documentId;
    }

    public double getScore() {
        return score;
    }
}
