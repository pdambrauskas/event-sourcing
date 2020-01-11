package com.github.pdambrauskas.eventsourcing.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

import java.io.IOException;
import java.nio.ByteBuffer;

public class JsonCodec<T> implements RedisCodec<String, T> {
    private final Class<T> clazz;
    private ObjectMapper mapper;

    public JsonCodec(ObjectMapper mapper, Class<T> clazz) {
        this.mapper = mapper;
        this.clazz = clazz;
    }

    public JsonCodec(Class<T> clazz) {
        this(new ObjectMapper(), clazz);
    }

    @Override
    public String decodeKey(ByteBuffer bytes) {
        return StringCodec.UTF8.decodeKey(bytes);
    }

    @Override
    public T decodeValue(ByteBuffer bytes) {
        try {
            return mapper.readValue(StringCodec.UTF8.decodeValue(bytes), clazz);
        } catch (IOException e) {
            throw new IllegalArgumentException("Illegal data", e);
        }
    }

    @Override
    public ByteBuffer encodeKey(String key) {
        return StringCodec.UTF8.encodeKey(key);
    }

    @Override
    public ByteBuffer encodeValue(T value) {
        try {
            return ByteBuffer.wrap(mapper.writeValueAsBytes(value));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Illegal data", e);
        }
    }
}
