package com.github.pdambrauskas.eventsourcing.redis.support.eventprocessing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.pdambrauskas.eventsourcing.redis.Snapshot;

public class CounterSnapshot implements Snapshot {
    private Long total;

    @JsonCreator
    public CounterSnapshot(@JsonProperty("total") Long total) {
        this.total = total;
    }

    public Long getTotal() {
        return total;
    }

    @Override
    public String toString() {
        return "CounterSnapshot{" +
                "total=" + total +
                '}';
    }
}

