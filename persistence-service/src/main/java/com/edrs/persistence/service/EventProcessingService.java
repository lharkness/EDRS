package com.edrs.persistence.service;

import com.edrs.persistence.entity.EventLog;
import com.edrs.persistence.entity.ProcessedEvent;
import com.edrs.persistence.mapper.EventLogMapper;
import com.edrs.persistence.mapper.ProcessedEventMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for handling event processing with idempotency and event sourcing.
 * Follows choreography pattern best practices.
 */
@Service
public class EventProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(EventProcessingService.class);
    private static final String SERVICE_NAME = "persistence-service";

    private final ProcessedEventMapper processedEventMapper;
    private final EventLogMapper eventLogMapper;
    private final ObjectMapper objectMapper;

    public EventProcessingService(
            ProcessedEventMapper processedEventMapper,
            EventLogMapper eventLogMapper,
            ObjectMapper objectMapper) {
        this.processedEventMapper = processedEventMapper;
        this.eventLogMapper = eventLogMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Checks if an event has already been processed (idempotency check).
     * 
     * @param eventId The unique event ID
     * @return true if event was already processed, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isEventProcessed(UUID eventId) {
        return processedEventMapper.existsByEventId(eventId);
    }

    /**
     * Marks an event as processed for idempotency.
     * 
     * @param eventId The unique event ID
     * @param correlationId The correlation ID for tracing
     * @param eventType The type of event
     */
    @Transactional
    public void markEventAsProcessed(UUID eventId, UUID correlationId, String eventType) {
        if (!processedEventMapper.existsByEventId(eventId)) {
            ProcessedEvent processedEvent = new ProcessedEvent();
            processedEvent.setEventId(eventId);
            processedEvent.setCorrelationId(correlationId);
            processedEvent.setEventType(eventType);
            processedEvent.setHandlerService(SERVICE_NAME);
            processedEvent.setProcessedAt(LocalDateTime.now());
            processedEventMapper.insert(processedEvent);
            logger.debug("Marked event {} as processed", eventId);
        }
    }

    /**
     * Logs an event to the event log for event sourcing.
     * 
     * @param eventId The unique event ID
     * @param correlationId The correlation ID
     * @param eventType The type of event
     * @param eventVersion The version of the event
     * @param source The source service
     * @param payload The event payload as JSON string
     */
    @Transactional
    public void logEvent(UUID eventId, UUID correlationId, String eventType, 
                        String eventVersion, String source, String payload) {
        try {
            EventLog eventLog = new EventLog();
            eventLog.setEventId(eventId);
            eventLog.setCorrelationId(correlationId);
            eventLog.setEventType(eventType);
            eventLog.setEventVersion(eventVersion);
            eventLog.setSource(source);
            eventLog.setPayload(payload);
            eventLog.setProcessed(false);
            eventLog.setTimestamp(LocalDateTime.now());
            eventLogMapper.insert(eventLog);
            logger.debug("Logged event {} to event log", eventId);
        } catch (Exception e) {
            logger.error("Error logging event to event log", e);
            // Don't throw - event logging failure shouldn't break processing
        }
    }

    /**
     * Marks an event log entry as processed.
     * 
     * @param eventId The unique event ID
     */
    @Transactional
    public void markEventLogAsProcessed(UUID eventId) {
        eventLogMapper.findByEventId(eventId).ifPresent(eventLog -> {
            eventLogMapper.updateProcessed(eventId, LocalDateTime.now());
        });
    }

    /**
     * Generates a unique event ID if not provided.
     * In a choreography pattern, events should have unique IDs for idempotency.
     */
    public UUID generateEventId() {
        return UUID.randomUUID();
    }
}
