package com.edrs.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks processed events for idempotency.
 * Prevents duplicate processing of the same event.
 */
public class ProcessedEvent {
    private Long id;
    private UUID eventId;
    private UUID correlationId;
    private String eventType;
    private LocalDateTime processedAt;
    private String handlerService;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getHandlerService() {
        return handlerService;
    }

    public void setHandlerService(String handlerService) {
        this.handlerService = handlerService;
    }
}
