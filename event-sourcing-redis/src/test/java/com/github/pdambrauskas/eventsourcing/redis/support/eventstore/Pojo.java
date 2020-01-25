package com.github.pdambrauskas.eventsourcing.redis.support.eventstore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.pdambrauskas.eventsourcing.redis.Event;

public class Pojo implements Event {
    private final String a;
    private final String id;

    @JsonCreator
    public Pojo(@JsonProperty("a") String a, @JsonProperty("id") String id) {
        this.a = a;
        this.id = id;
    }

    public String getA() {
        return a;
    }

    @Override
    public String getId() {
        return id;
    }
}