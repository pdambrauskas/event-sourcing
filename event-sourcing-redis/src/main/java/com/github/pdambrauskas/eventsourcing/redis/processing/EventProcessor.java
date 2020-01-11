package com.github.pdambrauskas.eventsourcing.redis.processing;

import com.github.pdambrauskas.eventsourcing.redis.ConsumedEvent;
import com.github.pdambrauskas.eventsourcing.redis.Event;
import com.github.pdambrauskas.eventsourcing.redis.SnapshotStore;

public interface EventProcessor<T extends Event> {
    String streamName();
    boolean applicableFor(ConsumedEvent<Event> event);
    void process(T event, SnapshotStore snapshotStore);
}
