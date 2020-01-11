package com.github.pdambrauskas.eventsourcing.redis.support.eventstore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.pdambrauskas.eventsourcing.redis.Event;

public class Pojo2 implements Event {
    private final String id;

    @JsonCreator
    public Pojo2(@JsonProperty("id") String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }
}
