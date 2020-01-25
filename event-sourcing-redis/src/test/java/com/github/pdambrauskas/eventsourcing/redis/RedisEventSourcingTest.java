package com.github.pdambrauskas.eventsourcing.redis;

import com.github.pdambrauskas.eventsourcing.redis.processing.StreamProcessor;
import com.github.pdambrauskas.eventsourcing.redis.support.eventprocessing.CounterEvent;
import com.github.pdambrauskas.eventsourcing.redis.support.eventprocessing.CounterSnapshot;
import com.github.pdambrauskas.eventsourcing.redis.support.eventprocessing.SumProcessor;
import com.github.pdambrauskas.eventsourcing.redis.support.eventstore.Pojo;
import com.github.pdambrauskas.eventsourcing.redis.support.eventstore.Pojo2;
import io.lettuce.core.RedisClient;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RedisEventSourcingTest {
    @Rule
    public GenericContainer<?> redis = new GenericContainer<>("redis:5.0.6-alpine")
            .waitingFor(Wait.forListeningPort())
            .withExposedPorts(6379);

    @Test
    public void testEventStore() {
        RedisClient redisCli = createCli();
        try (EventStore eventStore = new EventStore(redisCli, "test")) {
            // setup
            eventStore.publish("pojos", new Pojo("a", "id"));
            eventStore.publish("pojos2", new Pojo2("id2"));

            //when
            final List<Event> consumedEvents = new ArrayList<>();
            eventStore.subscribe("pojos", "pojos2").forEachRemaining(m -> consumedEvents.add(m.getBody()));

            // then
            assertEquals(2, consumedEvents.size());
            Pojo event = (Pojo) consumedEvents.get(0);
            assertEquals("id", event.getId());
            assertEquals("a", event.getA());
            assertEquals("id2", consumedEvents.get(1).getId());
        } finally {
            redisCli.shutdown();
        }
    }

    @Test
    public void testProcessor() {
        var sumProcessor = new SumProcessor("count");
        var redisCli = createCli();
        var processor = new StreamProcessor<CounterSnapshot>(redisCli, List.of(sumProcessor), "test");
        try (EventStore eventStore = new EventStore(redisCli, "test");
             SnapshotStore snapshotStore = new SnapshotStore(redisCli)) {
            // setup
            eventStore.publish(sumProcessor.streamName(), new CounterEvent("14", 12));
            eventStore.publish(sumProcessor.streamName(), new CounterEvent("14", 13));
            eventStore.publish(sumProcessor.streamName(), new CounterEvent("15", 1));

            // when
            processor.process(false);
            CounterSnapshot sum = snapshotStore.get("14");

            // then
            assertEquals(Long.valueOf(25), sum.getTotal());
        } finally {
            redisCli.shutdown();
        }
    }

    private RedisClient createCli() {
        return RedisClient.create("redis://" + redis.getContainerIpAddress() + ":" + redis.getFirstMappedPort());
    }
}