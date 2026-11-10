package com.searchSystem.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "documents")
public class Document implements Serializable {

    @Id
    private String id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private IndexStatus indexStatus;

    private LocalDateTime indexedAt;
    private static final long serialVersionUID = 1L;

    private long createdAt;

    private long popularityScore;

    public Document() {
    }

    public Document(
            String id,
            String content) {

        this.id = id;
        this.content = content;
        this.createdAt = System.currentTimeMillis();
        this.popularityScore = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public IndexStatus getIndexStatus() {
        return indexStatus;
    }

    public void setIndexStatus(
            IndexStatus indexStatus) {

        this.indexStatus = indexStatus;
    }

    public LocalDateTime getIndexedAt() {
        return indexedAt;
    }

    public void setIndexedAt(
            LocalDateTime indexedAt) {

        this.indexedAt = indexedAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            long createdAt) {

        this.createdAt = createdAt;
    }

    public long getPopularityScore() {
        return popularityScore;
    }

    public void incrementPopularity() {
        this.popularityScore++;
    }
}
