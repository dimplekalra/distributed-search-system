package com.searchSystem.model;

import com.searchSystem.index.IndexSegment;

import java.io.Serializable;
import java.util.List;

public class SnapshotData implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<IndexSegment> segments;

    public SnapshotData() {
    }

    public SnapshotData(List<IndexSegment> segments) {
        this.segments = segments;
    }

    public List<IndexSegment> getSegments() {
        return segments;
    }

    public void setSegments(
            List<IndexSegment> segments) {

        this.segments = segments;
    }

}
