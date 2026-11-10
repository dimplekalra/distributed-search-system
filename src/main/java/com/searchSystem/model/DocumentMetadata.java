package com.searchSystem.model;

import java.io.Serializable;

public class DocumentMetadata implements Serializable {
    private final int documentLength;

    private long popularityScore;

    private final long createdAt;

    public DocumentMetadata(
            int documentLength,
            long popularityScore,
            long createdAt) {

        this.documentLength = documentLength;
        this.popularityScore = popularityScore;
        this.createdAt = createdAt;
    }

    public int getDocumentLength() {
        return documentLength;
    }

    public long getPopularityScore() {
        return popularityScore;
    }

    public void incrementPopularity() {
        this.popularityScore++;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
