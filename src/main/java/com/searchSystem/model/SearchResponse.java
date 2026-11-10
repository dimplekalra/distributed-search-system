package com.searchSystem.model;

public class SearchResponse {
    private String documentId;
    private double score;
    private String content;
    private String snippet;

    public SearchResponse(
            String documentId,
            double score,
            String content,
            String snippet) {

        this.documentId = documentId;
        this.score = score;
        this.content = content;
        this.snippet = snippet;
    }

    public String getDocumentId() {
        return documentId;
    }

    public double getScore() {
        return score;
    }

    public String getContent() {
        return content;
    }

    public String getSnippet() {
        return snippet;
    }
};
