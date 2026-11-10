package com.searchSystem.model;

import java.time.LocalDateTime;

public class RetryableDocument {
    private Document document;
    private int retryCount;
    private LocalDateTime nextRetryTime;

    public RetryableDocument() {
    }

    public RetryableDocument(Document document, int retryCount) {
        this.document = document;
        this.retryCount = retryCount;
        this.nextRetryTime = LocalDateTime.now();
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getNextRetryTime() {
        return nextRetryTime;
    }

    public void setNextRetryTime(LocalDateTime nextRetryTime) {
        this.nextRetryTime = nextRetryTime;
    }
}
