package com.searchSystem.repository;

import com.searchSystem.model.Document;
import com.searchSystem.model.IndexStatus;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, String> {
    List<Document> findByIndexStatus(IndexStatus status);

    List<Document> findByContentContainingIgnoreCase(
            String keyword,
            Pageable pageable);
}
