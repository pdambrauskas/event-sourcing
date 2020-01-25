package com.github.pdambrauskas.eventsourcing.redis.processing;

import com.github.pdambrauskas.eventsourcing.redis.ConsumedEvent;
import com.github.pdambrauskas.eventsourcing.redis.Event;
import com.github.pdambrauskas.eventsourcing.redis.EventStore;
import com.github.pdambrauskas.eventsourcing.redis.SnapshotStore;
import io.lettuce.core.RedisClient;

import java.util.Collection;
import java.util.Iterator;

public class StreamProcessor<T> {
    private final Collection<? extends EventProcessor> eventProcessors;
    private final RedisClient redis;
    private final String consumerId;

    public StreamProcessor(RedisClient redis,
                           Collection<? extends EventProcessor> eventProcessors,
                           String consumerId) {
        this.redis = redis;
        this.eventProcessors = eventProcessors;
        this.consumerId = consumerId;
    }

    public void process(boolean waitForNewMessages) {
        String[] streamNames = eventProcessors.stream().map(EventProcessor::streamName).toArray(String[]::new);
        processStreams(streamNames, waitForNewMessages);
    }

    private void processStreams(String[] streamNames, boolean waitForNewMessages) {
        try (var eventStore = new EventStore(redis, consumerId);
             var snapshotStore = new SnapshotStore(redis)) {
            do {
                Iterator<ConsumedEvent<Event>> iterator = eventStore.subscribe(streamNames);
                iterator.forEachRemaining(event ->
                        eventProcessors.stream().filter(p -> p.applicableFor(event))
                                .forEach(p -> {
                                    p.process(event.getBody(), snapshotStore);
                                    eventStore.ack(event);
                                })
                );
            } while (waitForNewMessages);

        }
    }

}
