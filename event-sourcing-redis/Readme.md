# Event Sourcing with Redis
It is a common practice to use asynchronous messaging technologies to implement communication between multiple microservices.
Kafka is a go-to solution when it comes to streaming pipelines and publish/subscribe systems for async communication. It has many advantages over traditional, synchronous HTTP communication, some of them are:
- You can easily scale microservice architecture by running multiple instances of your microservice in the same consumer group.
- You can add new consumers without modifying existing communication-related code.
- Take advantage of persistence of Kafka topics by re-consuming them any time you need.

I have implemented asynchronous communication using Kafka several times. Both at work and on my personal projects.
 But there are many alternatives. One of the latest implementations is newest Redis Datatype - Redis Streams, which came in [Redis 5.0](https://redislabs.com/blog/redis-5-0-is-here/).

[Redis Streams](https://redis.io/topics/streams-intro) may look very similar to traditional Redis Pub/Sub concept, however it is quite different. It shares main conceptual ideas with Apache Kafka:
- Stream can have multiple consumers, every new entry on a stream will be delivered to every consumer (unless consumers belong to same consumer group).
- Consumed messages do not disappear, Redis stores streamed data and last consumed *id* for each consumer group,
 so new consumer groups can consume group from the beginning.
 
 So, as you see, in theory Redis Streams work very similar to Kafka topics, which means it can be used for same use cases.
 You can find many resources and examples on how to use Kafka for Event Sourcing ([example](https://www.confluent.io/blog/event-sourcing-cqrs-stream-processing-apache-kafka-whats-connection/)),
 but when you look at Redis Streams, the variety of examples is very limited. So I decided to contribute to filling in this resource gap :).
 In this post I will try to describe how to use Redis Streams for [Event sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)
 & [CQRS](https://www.martinfowler.com/bliki/CQRS.html).
 
 ## Redis Streams API theoretical example
 Lets say we have a REST API endpoint for GET'ing user social media profile, with personal information, liked pages and list of users' friends.
 For that we'll have three different Redis Streams:
 - `user_updates` stream consists of user profile updates. Every time the user changes his birth date, name or other personal information, entry is published to this stream.
 - `user_activity` every time user writes a comment, reacts to some post, or performs any other action (that is available in Facebook), entry is published to this stream.
 - `user_friends` every time user gets or loses a friend, entry is published to this stream.
 
### Filling up event streams
So, we would publish our events by using `XADD` command:
```shell script
XADD user_updates * user_id 1 command register name Duffy surename Duck
XADD user_updates * user_id 2 command register name Bugs surename Bunny
XADD user_activity * user_id 1 command kick object Bugs
XADD user_activity * user_id 1 command dislike object Bugs face
XADD user_friends * user_id 1 command remove friend_id 2
```

### Reading messages from event streams
Now we can read all streams using `XREAD` command:
```shell script
XREAD STREAMS user_updates user_activity user_friends 0 0 0
```

Your output should look like this:
```shell script
1) 1) "user_updates"
   2) 1) 1) "1577650357114-0"
         2) 1) "user_id"
            2) "1"
            3) "action"
            4) "register"
            5) "name"
            6) "Duffy"
            7) "surename"
            8) "Duck"
      2) 1) "1577650371803-0"
         2) 1) "user_id"
            2) "2"
            3) "action"
            4) "register"
            5) "name"
            6) "Bugs"
            7) "surename"
            8) "Bunny"
2) 1) "user_activity"
   2) 1) 1) "1577650378926-0"
         2) 1) "user_id"
            2) "1"
            3) "action"
            4) "kick"
            5) "object"
            6) "Bugs"
      2) 1) "1577650384649-0"
         2) 1) "user_id"
            2) "1"
            3) "action"
            4) "dislike"
            5) "object"
            6) "Bugs face"
3) 1) "user_friends"
   2) 1) 1) "1577650389616-0"
         2) 1) "user_id"
            2) "1"
            3) "action"
            4) "remove"
            5) "friend_id"
            6) "2"
```

### Building snapshots
When building profile representation, you can transform and reflect it in any structure you need.
Notice, the three zeroes at the end of the `XREAD` command. Those are entry IDs, that can be used as consumer offsets when you are using `XREADGROUP` command (we'll use this command later).
Entry IDs by default are actually timestamps, with nanoseconds part after dot, so it is easy to read stream from any point in time you need. 

In most cases, it is inefficient to consume the whole stream every time you need data. It is a common practice to save your user profile snapshots in one way or another.
For snapshoting you can use Redis Hash data structure (or one of other Redis structures, if that makes sense for you).
Hash structure can be created by using `HMSET` command
(you can set multiple hash fields for aggregated data, it is skipped in this example), the suffix of Hash key is user id:
```shell script
HMSET user_snapshot_1 name Duffy ... [field value]
```

### Continuous consumption
But you don't want to keep track of offsets, you have already consumed, right? Redis has a solution to this problem too.
What you can do is use `XREADGROUP` along with `XACK`. Pseudocode for whole snapshoting process would look something like this:
```
XGROUP CREATE user_updates snapshotter 0
XGROUP CREATE user_activity snapshotter 0
XGROUP CREATE user_friends snapshotter 0
WHILE true
    entries = XREADGROUP GROUP snapshotter event_consumer BLOCK 2000 COUNT 10 STREAMS user_updates user_activity user_friends > > >
    if entries == nil
        puts "Timeout... try again"
        CONTINUE
    end

    FOREACH entries AS stream_entries
        FOREACH stream_entries as message
            process_message(message)
            XACK message.stream snapshotter message.id
        END
    END
END
```

## Java example

I've also implemented Java App to illustrate how to use Redis Streams for Event Sourcing. I chose to use [lettuce](https://github.com/lettuce-io/lettuce-core) library for communication with Redis server.
Source code of my implementation can be found on [Github](https://github.com/pdambrauskas/event-sourcing/tree/master/event-sourcing-redis)

There are three main classes:
- [EventStore](https://github.com/pdambrauskas/event-sourcing/blob/master/event-sourcing-redis/src/main/java/com/github/pdambrauskas/eventsourcing/redis/EventStore.java) - this class can be used for event publishing, and subscribing to Redis Streams.
- [SnapshotStore](https://github.com/pdambrauskas/event-sourcing/blob/master/event-sourcing-redis/src/main/java/com/github/pdambrauskas/eventsourcing/redis/SnapshotStore.java) - this class can be used to store and retrieve snapshot objects from Redis.
- [StreamProcessor](https://github.com/pdambrauskas/event-sourcing/blob/master/event-sourcing-redis/src/main/java/com/github/pdambrauskas/eventsourcing/redis/processing/StreamProcessor.java) - combines EventStore and Snapshot store. You can supply multiple event handlers, which are used for building snapshots.

The whole combination of these classes can be found on [RedisEventSourcingTest](https://github.com/pdambrauskas/event-sourcing/blob/master/event-sourcing-redis/src/test/java/com/github/pdambrauskas/eventsourcing/redis/RedisEventSourcingTest.java) Unit test class.

## Conclusion
While Redis Streams is relatively new concept, it is heavily inspired by Apache Kafka, and has many overlapping features. Also we mustn't forget that
Redis has many more data structures and features, which can be used alongside Redis streams (one of which we used for the snapshotting in this post).
If you already have Redis in your technology stack and are looking into streaming solutions, consider using Redis Streams.
Not only Redis is feature reach it is also very easy to learn and use.  
