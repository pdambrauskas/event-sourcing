package com.github.pdambrauskas.eventsourcing.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;

public class SnapshotStore implements AutoCloseable {
    private final StatefulRedisConnection<String, Snapshot> connection;

    public SnapshotStore(RedisClient redisClient) {
        RedisCodec<String, Snapshot> codec = new JsonCodec<>(Snapshot.class);
        connection = redisClient.connect(codec);
    }

    public void store(String key, Snapshot snapshot) {
        connection.sync().set(key, snapshot);
    }

    public <T extends Snapshot> T get(String key) {
        return (T) connection.sync().get(key);
    }

    @Override
    public void close() {
        connection.close();
    }
}
