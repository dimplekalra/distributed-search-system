package com.searchSystem.index;

import com.searchSystem.model.Posting;
import java.io.Serializable;
import java.util.*;

public class InvertedIndex implements Serializable {

    private Map<String, List<Posting>> index = new HashMap<>();

    public void add(String term, String documentId, int position) {

        List<Posting> postings = index.computeIfAbsent(term, k -> new ArrayList<>());

        Posting posting = null;

        for (Posting existing : postings) {
            if (existing.getDocumentId().equals(documentId)) {
                posting = existing;
                break;
            }
        }

        if (posting == null) {

            posting = new Posting(documentId, 0, new ArrayList<>());

            postings.add(posting);
        }

        posting.addPosition(position);
    }

    public List<Posting> getPostings(String term) {
        return index.getOrDefault(term, Collections.emptyList());
    }

    public int getDocumentFrequency(String term) {
        return index.getOrDefault(term, Collections.emptyList()).size();
    }

    public Map<String, List<Posting>> getIndex() {
        return index;
    }

    public void setIndex(Map<String, List<Posting>> index) {
        this.index = index;
    }

    public void merge(InvertedIndex other) {

        for (Map.Entry<String, List<Posting>> entry : other.getIndex().entrySet()) {

            List<Posting> postings = index.computeIfAbsent(
                    entry.getKey(),
                    k -> new ArrayList<>());

            postings.addAll(entry.getValue());
        }
    }
}