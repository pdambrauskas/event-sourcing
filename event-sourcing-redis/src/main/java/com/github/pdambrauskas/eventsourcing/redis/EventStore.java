package com.github.pdambrauskas.eventsourcing.redis;

import io.lettuce.core.Consumer;
import io.lettuce.core.RedisBusyException;
import io.lettuce.core.RedisClient;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs.StreamOffset;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.RedisCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Iterator;

public class EventStore implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(EventStore.class);
    private static final String CONSUMER_NAME = "eventstore";
    static final String BODY_KEY = "body";

    private final String consumerGroupId;
    private final StatefulRedisConnection<String, Event> connection;
    private final RedisCommands<String, Event> commands;

    public EventStore(RedisClient redis, String consumerGroupId) {
        RedisCodec<String, Event> codec = new JsonCodec<>(Event.class);
        connection = redis.connect(codec);
        commands = connection.sync();
        this.consumerGroupId = consumerGroupId;
    }

    public void ensureGroup(String eventName) {
        try {
            commands.xgroupCreate(StreamOffset.latest(eventName),
                    consumerGroupId,
                    XGroupCreateArgs.Builder.mkstream(true));
        } catch (RedisBusyException e) {
            // Group exists
        }
    }

    public void publish(String eventName, Event event) {
        ensureGroup(eventName);
        commands.xadd(eventName, BODY_KEY, event);
    }

    public Iterator<ConsumedEvent<Event>> subscribe(String... eventNames) {
        @SuppressWarnings("unchecked")
        StreamOffset<String>[] streams = Arrays.stream(eventNames)
                .map(StreamOffset::lastConsumed)
                .peek(stream -> LOG.debug("Subscribed to: {}", stream))
                .toArray(StreamOffset[]::new);

        return new StreamIterator<>(() ->
                commands.xreadgroup(Consumer.from(consumerGroupId, CONSUMER_NAME), streams));
    }

    public void ack(ConsumedEvent<Event> event) {
        commands.xack(event.getStream(), consumerGroupId, event.getId());
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (Exception e) {
            // ignore
        }
    }
}
