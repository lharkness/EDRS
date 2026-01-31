package com.edrs.persistence.listener;

import com.edrs.common.events.CancellationRequestedEvent;
import com.edrs.common.events.InventoryReceivedEvent;
import com.edrs.common.events.ReservationRequestedEvent;
import com.edrs.persistence.service.EventProcessingService;
import com.edrs.persistence.service.PersistenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Event listener for persistence service following choreography pattern.
 * Handles events from Kafka and delegates to persistence service with idempotency.
 */
@Component
public class PersistenceEventListener {
    private static final Logger logger = LoggerFactory.getLogger(PersistenceEventListener.class);
    private static final String RESERVATION_REQUESTED_TOPIC = "reservation-requested";
    private static final String CANCELLATION_REQUESTED_TOPIC = "cancellation-requested";
    private static final String INVENTORY_RECEIVED_TOPIC = "inventory-received";

    private final PersistenceService persistenceService;
    private final EventProcessingService eventProcessingService;
    private final ObjectMapper objectMapper;

    public PersistenceEventListener(
            PersistenceService persistenceService,
            EventProcessingService eventProcessingService,
            ObjectMapper objectMapper) {
        this.persistenceService = persistenceService;
        this.eventProcessingService = eventProcessingService;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles reservation requested events.
     * In a full implementation, eventId would come from event headers or payload.
     */
    @KafkaListener(topics = RESERVATION_REQUESTED_TOPIC, groupId = "persistence-service-group")
    public void handleReservationRequested(
            ConsumerRecord<String, String> record,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        UUID eventId = null;
        try {
            ReservationRequestedEvent event = objectMapper.readValue(record.value(), ReservationRequestedEvent.class);
            
            // In a choreography pattern, eventId should be in headers or event payload
            // For now, generate from correlationId + timestamp for idempotency
            // In production, events should include eventId
            eventId = generateEventIdFromRecord(record, event.getCorrelationId());
            
            logger.info("Received reservation requested event: correlationId={}, eventId={}", 
                       event.getCorrelationId(), eventId);
            
            persistenceService.processReservationRequest(event, eventId);
            
            // Acknowledge message after successful processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            logger.error("Error processing reservation requested event: eventId={}", eventId, e);
            // In production, implement dead letter queue pattern here
            // For now, we'll let Kafka retry or move to DLQ
        }
    }

    @KafkaListener(topics = CANCELLATION_REQUESTED_TOPIC, groupId = "persistence-service-group")
    public void handleCancellationRequested(
            ConsumerRecord<String, String> record,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        UUID eventId = null;
        try {
            CancellationRequestedEvent event = objectMapper.readValue(record.value(), CancellationRequestedEvent.class);
            
            eventId = generateEventIdFromRecord(record, event.getCorrelationId());
            
            logger.info("Received cancellation requested event: correlationId={}, eventId={}", 
                       event.getCorrelationId(), eventId);
            
            persistenceService.processCancellationRequest(event, eventId);
            
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            logger.error("Error processing cancellation requested event: eventId={}", eventId, e);
        }
    }

    @KafkaListener(topics = INVENTORY_RECEIVED_TOPIC, groupId = "persistence-service-group")
    public void handleInventoryReceived(
            ConsumerRecord<String, String> record,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        UUID eventId = null;
        try {
            InventoryReceivedEvent event = objectMapper.readValue(record.value(), InventoryReceivedEvent.class);
            
            eventId = generateEventIdFromRecord(record, event.getCorrelationId());
            
            logger.info("Received inventory received event: correlationId={}, eventId={}", 
                       event.getCorrelationId(), eventId);
            
            persistenceService.processInventoryReceived(event, eventId);
            
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            logger.error("Error processing inventory received event: eventId={}", eventId, e);
        }
    }

    /**
     * Generates a deterministic event ID from Kafka record for idempotency.
     * In production, events should include eventId in payload or headers.
     * This uses partition + offset + correlationId to create a unique, deterministic ID.
     */
    private UUID generateEventIdFromRecord(ConsumerRecord<String, String> record, UUID correlationId) {
        // Use partition, offset, and correlationId to create deterministic event ID
        // This ensures the same event always gets the same ID for idempotency
        String idString = String.format("%s-%d-%d-%s", 
            record.topic(), 
            record.partition(), 
            record.offset(),
            correlationId.toString());
        
        // Generate UUID from string (deterministic)
        return UUID.nameUUIDFromBytes(idString.getBytes());
    }
}
