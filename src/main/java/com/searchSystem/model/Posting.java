package com.searchSystem.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Posting implements Serializable {

    private static final long serialVersionUID = 1L;
    private String documentId;
    private int termFrequency;
    private List<Integer> positions;

    public Posting(String docId, int termFreq, List<Integer> positions) {
        this.documentId = docId;
        this.termFrequency = termFreq;
        this.positions = positions;
    }

    public String getDocumentId() {
        return documentId;
    }

    public int getTermFrequency() {
        return termFrequency;
    }

    public List<Integer> getPositions() {
        return positions;
    }

    public void addPosition(int position) {
        if (positions == null) {
            positions = new ArrayList<>();
        }

        positions.add(position);
        termFrequency++;
    }
}
