package com.github.pdambrauskas.eventsourcing.redis;

import io.lettuce.core.StreamMessage;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import static com.github.pdambrauskas.eventsourcing.redis.EventStore.BODY_KEY;

public class StreamIterator<T> implements Iterator<ConsumedEvent<T>> {
    private List<StreamMessage<String, T>> currentBatch;
    private List<StreamMessage<String, T>> nextBatch;
    private int currentBatchIndex;

    private Supplier<List<StreamMessage<String, T>>> batchProvider;

    public StreamIterator(Supplier<List<StreamMessage<String, T>>> batchProvider) {
        this.batchProvider = batchProvider;
        this.currentBatch = batchProvider.get();
        this.nextBatch = batchProvider.get();
        this.currentBatchIndex = 0;
    }

    @Override
    public boolean hasNext() {
        return currentBatch.size() > currentBatchIndex || !nextBatch.isEmpty();
    }

    @Override
    public ConsumedEvent<T> next() {
        if (currentBatchIndex < currentBatch.size()) {
            return toEvent(currentBatch.get(currentBatchIndex++));
        }

        currentBatch = nextBatch;
        nextBatch = batchProvider.get();
        currentBatchIndex = 0;
        return next();
    }

    private ConsumedEvent<T> toEvent(StreamMessage<String, T> message) {
        return  ConsumedEvent.of(message.getStream(), message.getId(), message.getBody().get(BODY_KEY));
    }
}
