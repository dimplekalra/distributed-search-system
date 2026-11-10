package com.searchSystem.model;

public class Suggestion {
    private String query;
    private int frequency;

    public Suggestion(String query, int frequency) {
        this.query = query;
        this.frequency = frequency;
    }

    public String getQuery() {
        return query;
    }

    public int getFrequency() {
        return frequency;
    }
}
