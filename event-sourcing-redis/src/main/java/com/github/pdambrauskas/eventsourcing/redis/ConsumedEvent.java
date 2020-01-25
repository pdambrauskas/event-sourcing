package com.github.pdambrauskas.eventsourcing.redis;

public class ConsumedEvent<T> {
    private final String id;
    private String stream;
    private T body;

    public static <T> ConsumedEvent<T> of(String stream, String id, T body) {
        return new ConsumedEvent<>(stream, id, body);
    }

    private ConsumedEvent(String stream, String id, T body) {
        this.stream = stream;
        this.id = id;
        this.body = body;
    }

    public String getStream() {
        return stream;
    }

    public String getId() {
        return id;
    }

    public T getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "ConsumedEvent{" +
                "stream='" + stream + '\'' +
                ", body=" + body +
                '}';
    }
}
