package com.github.pdambrauskas.eventsourcing.redis.support.eventprocessing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.pdambrauskas.eventsourcing.redis.Event;

public class CounterEvent implements Event {
    private final String id;
    private Integer count;

    @JsonCreator
    public CounterEvent(@JsonProperty("id") String id, @JsonProperty("count") Integer count) {
        this.id = id;
        this.count = count;
    }

    @Override
    public String getId() {
        return id;
    }

    public Integer getCount() {
        return count;
    }
}
