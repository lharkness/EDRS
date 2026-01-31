package com.edrs.common.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all events in the choreography-style event-driven architecture.
 * Provides common fields for event versioning, idempotency, and correlation tracking.
 */
public abstract class BaseEvent {
    private final UUID eventId;
    private final UUID correlationId;
    private final String eventType;
    private final String eventVersion;
    private final LocalDateTime timestamp;
    private final String source;

    protected BaseEvent(
            @JsonProperty("eventId") UUID eventId,
            @JsonProperty("correlationId") UUID correlationId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("eventVersion") String eventVersion,
            @JsonProperty("timestamp") LocalDateTime timestamp,
            @JsonProperty("source") String source) {
        this.eventId = eventId;
        this.correlationId = correlationId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.timestamp = timestamp;
        this.source = source;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventVersion() {
        return eventVersion;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getSource() {
        return source;
    }
}
