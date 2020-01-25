package com.github.pdambrauskas.eventsourcing.redis.support.eventprocessing;

import com.github.pdambrauskas.eventsourcing.redis.ConsumedEvent;
import com.github.pdambrauskas.eventsourcing.redis.Event;
import com.github.pdambrauskas.eventsourcing.redis.SnapshotStore;
import com.github.pdambrauskas.eventsourcing.redis.processing.EventProcessor;

import static java.util.Optional.ofNullable;

public class SumProcessor implements EventProcessor<CounterEvent> {
    private String stream;

    public SumProcessor(String stream) {
        this.stream = stream;
    }

    @Override
    public String streamName() {
        return stream;
    }

    @Override
    public boolean applicableFor(ConsumedEvent<Event> event) {
        return streamName().equals(event.getStream());
    }

    @Override
    public void process(CounterEvent event, SnapshotStore snapshotStore) {
        String id = event.getId();
        CounterSnapshot currentSnapshot = snapshotStore.get(id);

        CounterSnapshot newSnapshot = new CounterSnapshot(ofNullable(currentSnapshot).map(CounterSnapshot::getTotal)
                .map(i -> i + event.getCount())
                .orElse(event.getCount().longValue()));
        System.out.println("STORE: " + id+ " curr: " + currentSnapshot + " new " + newSnapshot);
        snapshotStore.store(id, newSnapshot);
    }
}
