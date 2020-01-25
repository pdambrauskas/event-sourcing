package com.github.pdambrauskas.eventsourcing.redis;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, property="@class")
public interface Snapshot {
}