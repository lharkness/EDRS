package com.edrs.persistence.service;

import com.edrs.common.events.CancellationRequestedEvent;
import com.edrs.common.events.CancellationSuccessfulEvent;
import com.edrs.common.events.InventoryReceivedEvent;
import com.edrs.common.events.ReservationCreatedEvent;
import com.edrs.common.events.ReservationFailedEvent;
import com.edrs.common.events.ReservationRequestedEvent;
import com.edrs.common.util.CorrelationIdUtil;
import com.edrs.persistence.entity.InventoryItem;
import com.edrs.persistence.entity.Reservation;
import com.edrs.persistence.mapper.InventoryItemMapper;
import com.edrs.persistence.mapper.ReservationMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persistence service following choreography pattern best practices:
 * - Idempotent event processing
 * - Event sourcing via event log
 * - Transactional event publishing (outbox pattern ready)
 * - Proper error handling
 */
@Service
public class PersistenceService {
    private static final Logger logger = LoggerFactory.getLogger(PersistenceService.class);
    private static final String RESERVATION_CREATED_TOPIC = "reservation-created";
    private static final String RESERVATION_FAILED_TOPIC = "reservation-failed";
    private static final String CANCELLATION_SUCCESSFUL_TOPIC = "cancellation-successful";
    private static final String INVENTORY_PERSISTED_TOPIC = "inventory-persisted";
    private static final String EVENT_VERSION = "1.0";

    private final ReservationMapper reservationMapper;
    private final InventoryItemMapper inventoryItemMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final EventProcessingService eventProcessingService;
    private final Tracer tracer;
    private final Meter meter;
    
    // Metrics
    private final LongCounter eventsProcessedCounter;
    private final LongCounter eventsFailedCounter;
    private final LongCounter reservationsCreatedCounter;
    private final LongCounter reservationsFailedCounter;
    private final LongCounter cancellationsProcessedCounter;
    private final LongCounter inventoryUpdatesCounter;

    @Autowired
    public PersistenceService(
            ReservationMapper reservationMapper,
            InventoryItemMapper inventoryItemMapper,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            EventProcessingService eventProcessingService,
            Tracer tracer,
            Meter meter) {
        this.reservationMapper = reservationMapper;
        this.inventoryItemMapper = inventoryItemMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.eventProcessingService = eventProcessingService;
        this.tracer = tracer;
        this.meter = meter;
        
        // Initialize metrics
        this.eventsProcessedCounter = meter.counterBuilder("edrs.events.processed")
                .setDescription("Total number of events processed")
                .build();
        this.eventsFailedCounter = meter.counterBuilder("edrs.events.failed")
                .setDescription("Total number of events that failed processing")
                .build();
        this.reservationsCreatedCounter = meter.counterBuilder("edrs.reservations.created")
                .setDescription("Total number of reservations created")
                .build();
        this.reservationsFailedCounter = meter.counterBuilder("edrs.reservations.failed")
                .setDescription("Total number of reservations that failed")
                .build();
        this.cancellationsProcessedCounter = meter.counterBuilder("edrs.cancellations.processed")
                .setDescription("Total number of cancellations processed")
                .build();
        this.inventoryUpdatesCounter = meter.counterBuilder("edrs.inventory.updates")
                .setDescription("Total number of inventory updates")
                .build();
    }

    /**
     * Processes reservation request event (idempotent).
     * In choreography pattern, this service reacts to events and publishes new events.
     */
    @Transactional
    public void processReservationRequest(ReservationRequestedEvent event, UUID eventId) {
        long startTime = System.currentTimeMillis();
        Span span = tracer.spanBuilder("processReservationRequest")
                .setAttribute("event.id", eventId.toString())
                .setAttribute("event.type", "ReservationRequestedEvent")
                .setAttribute("correlation.id", event.getCorrelationId().toString())
                .setAttribute("user.id", event.getUserId())
                .setAttribute("inventory.items.count", event.getInventoryItemQuantities().size())
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            CorrelationIdUtil.setCorrelationId(event.getCorrelationId());
            logger.info("Processing reservation request with correlationId: {}, eventId: {}", 
                       event.getCorrelationId(), eventId);

            // Idempotency check - if already processed, skip
            if (eventProcessingService.isEventProcessed(eventId)) {
                logger.info("Event {} already processed, skipping (idempotency)", eventId);
                span.setAttribute("event.processed", true);
                span.setAttribute("event.skipped", true);
                return;
            }

            // Log event for event sourcing
            String eventPayload = objectMapper.writeValueAsString(event);
            eventProcessingService.logEvent(
                eventId,
                event.getCorrelationId(),
                "ReservationRequestedEvent",
                EVENT_VERSION,
                "reservation-service",
                eventPayload
            );

            // Check inventory availability for each item on the requested date
            Span availabilitySpan = tracer.spanBuilder("checkInventoryAvailability")
                    .setParent(io.opentelemetry.context.Context.current().with(span))
                    .startSpan();
            String unavailabilityReason;
            try (Scope availabilityScope = availabilitySpan.makeCurrent()) {
                unavailabilityReason = checkInventoryAvailability(event.getInventoryItemQuantities(), event.getReservationDate());
                availabilitySpan.setAttribute("availability.available", unavailabilityReason == null);
                if (unavailabilityReason != null) {
                    availabilitySpan.setAttribute("availability.reason", unavailabilityReason);
                }
            } finally {
                availabilitySpan.end();
            }
            
            if (unavailabilityReason != null) {
                // Mark event as processed (even though it failed)
                eventProcessingService.markEventAsProcessed(
                    eventId,
                    event.getCorrelationId(),
                    "ReservationRequestedEvent"
                );
                eventProcessingService.markEventLogAsProcessed(eventId);
                
                // Publish reservation failed event
                ReservationFailedEvent failedEvent = new ReservationFailedEvent(
                        event.getCorrelationId(),
                        event.getUserId(),
                        event.getInventoryItemQuantities(),
                        event.getReservationDate(),
                        unavailabilityReason,
                        LocalDateTime.now()
                );
                
                publishEvent(RESERVATION_FAILED_TOPIC, event.getCorrelationId().toString(), failedEvent);
                logger.warn("Reservation failed due to unavailability: {}", unavailabilityReason);
                
                // Metrics
                eventsProcessedCounter.add(1, Attributes.of(
                        io.opentelemetry.api.common.AttributeKey.stringKey("event.type"), "ReservationRequestedEvent",
                        io.opentelemetry.api.common.AttributeKey.stringKey("status"), "failed"
                ));
                reservationsFailedCounter.add(1);
                span.setAttribute("reservation.status", "failed");
                span.setAttribute("reservation.failure.reason", unavailabilityReason);
                return; // Exit early - don't create reservation
            }

            // Generate confirmation number
            String confirmationNumber = UUID.randomUUID().toString();
            
            // Persist reservation
            Reservation reservation = new Reservation();
            reservation.setConfirmationNumber(confirmationNumber);
            reservation.setUserId(event.getUserId());
            reservation.setReservationDate(event.getReservationDate());
            reservation.setStatus("CONFIRMED");
            reservation.setCreatedAt(LocalDateTime.now());
            reservation.setUpdatedAt(LocalDateTime.now());
            
            reservationMapper.insert(reservation);
            
            // Insert reservation items with quantities
            for (Map.Entry<String, Integer> entry : event.getInventoryItemQuantities().entrySet()) {
                reservationMapper.insertReservationItem(confirmationNumber, entry.getKey(), entry.getValue());
            }
            
            logger.info("Persisted reservation with confirmation number: {}", confirmationNumber);
            
            // Mark event as processed
            eventProcessingService.markEventAsProcessed(
                eventId,
                event.getCorrelationId(),
                "ReservationRequestedEvent"
            );
            eventProcessingService.markEventLogAsProcessed(eventId);
            
            // Publish reservation created event (choreography - other services react to this)
            ReservationCreatedEvent createdEvent = new ReservationCreatedEvent(
                    event.getCorrelationId(),
                    confirmationNumber,
                    event.getUserId(),
                    event.getInventoryItemQuantities(),
                    event.getReservationDate(),
                    LocalDateTime.now()
            );
            
            publishEvent(RESERVATION_CREATED_TOPIC, event.getCorrelationId().toString(), createdEvent);
            logger.info("Published reservation created event with correlationId: {}", event.getCorrelationId());
            
            // Metrics
            long processingTime = System.currentTimeMillis() - startTime;
            eventsProcessedCounter.add(1, Attributes.of(
                    io.opentelemetry.api.common.AttributeKey.stringKey("event.type"), "ReservationRequestedEvent",
                    io.opentelemetry.api.common.AttributeKey.stringKey("status"), "success"
            ));
            reservationsCreatedCounter.add(1);
            span.setAttribute("reservation.status", "created");
            span.setAttribute("reservation.confirmation.number", confirmationNumber);
            span.setAttribute("processing.time.ms", processingTime);
            
        } catch (Exception e) {
            logger.error("Error processing reservation request for eventId: {}", eventId, e);
            eventsFailedCounter.add(1, Attributes.of(
                    io.opentelemetry.api.common.AttributeKey.stringKey("event.type"), "ReservationRequestedEvent",
                    io.opentelemetry.api.common.AttributeKey.stringKey("error.type"), e.getClass().getSimpleName()
            ));
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            throw new RuntimeException("Failed to process reservation request", e);
        } finally {
            span.end();
        }
    }

    /**
     * Processes cancellation request event (idempotent).
     */
    @Transactional
    public void processCancellationRequest(CancellationRequestedEvent event, UUID eventId) {
        long startTime = System.currentTimeMillis();
        Span span = tracer.spanBuilder("processCancellationRequest")
                .setAttribute("event.id", eventId.toString())
                .setAttribute("event.type", "CancellationRequestedEvent")
                .setAttribute("correlation.id", event.getCorrelationId().toString())
                .setAttribute("confirmation.number", event.getConfirmationNumber())
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            CorrelationIdUtil.setCorrelationId(event.getCorrelationId());
            logger.info("Processing cancellation request with correlationId: {}, eventId: {}", 
                       event.getCorrelationId(), eventId);

            // Idempotency check
            if (eventProcessingService.isEventProcessed(eventId)) {
                logger.info("Event {} already processed, skipping (idempotency)", eventId);
                span.setAttribute("event.skipped", true);
                return;
            }

            try {
                // Log event
                String eventPayload = objectMapper.writeValueAsString(event);
                eventProcessingService.logEvent(
                    eventId,
                    event.getCorrelationId(),
                    "CancellationRequestedEvent",
                    EVENT_VERSION,
                    "reservation-service",
                    eventPayload
                );

                // Find and update reservation
                Reservation reservation = reservationMapper.findByConfirmationNumber(event.getConfirmationNumber());
                if (reservation == null) {
                    throw new RuntimeException("Reservation not found: " + event.getConfirmationNumber());
                }
                
                reservation.setStatus("CANCELLED");
                reservation.setUpdatedAt(LocalDateTime.now());
                reservationMapper.update(reservation);
                logger.info("Updated reservation {} status to CANCELLED", event.getConfirmationNumber());
                
                // Mark event as processed
                eventProcessingService.markEventAsProcessed(
                    eventId,
                    event.getCorrelationId(),
                    "CancellationRequestedEvent"
                );
                eventProcessingService.markEventLogAsProcessed(eventId);
                
                // Publish cancellation successful event
                CancellationSuccessfulEvent successfulEvent = new CancellationSuccessfulEvent(
                        event.getCorrelationId(),
                        event.getConfirmationNumber(),
                        reservation.getUserId(),
                        LocalDateTime.now()
                );
                
                publishEvent(CANCELLATION_SUCCESSFUL_TOPIC, event.getCorrelationId().toString(), successfulEvent);
                logger.info("Published cancellation successful event with correlationId: {}", event.getCorrelationId());
                
                // Metrics
                long processingTime = System.currentTimeMillis() - startTime;
                eventsProcessedCounter.add(1, Attributes.of(
                        io.opentelemetry.api.common.AttributeKey.stringKey("event.type"), "CancellationRequestedEvent",
                        io.opentelemetry.api.common.AttributeKey.stringKey("status"), "success"
                ));
                cancellationsProcessedCounter.add(1);
                span.setAttribute("cancellation.status", "successful");
                span.setAttribute("processing.time.ms", processingTime);
                
            } catch (Exception e) {
                logger.error("Error processing cancellation request for eventId: {}", eventId, e);
                eventsFailedCounter.add(1, Attributes.of(
                        io.opentelemetry.api.common.AttributeKey.stringKey("event.type"), "CancellationRequestedEvent",
                        io.opentelemetry.api.common.AttributeKey.stringKey("error.type"), e.getClass().getSimpleName()
                ));
                span.recordException(e);
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
                throw new RuntimeException("Failed to process cancellation request", e);
            }
        } finally {
            span.end();
        }
    }

    /**
     * Processes inventory received event (idempotent).
     */
    @Transactional
    public void processInventoryReceived(InventoryReceivedEvent event, UUID eventId) {
        long startTime = System.currentTimeMillis();
        Span span = tracer.spanBuilder("processInventoryReceived")
                .setAttribute("event.id", eventId.toString())
                .setAttribute("event.type", "InventoryReceivedEvent")
                .setAttribute("correlation.id", event.getCorrelationId().toString())
                .setAttribute("inventory.records.count", event.getReceiveRecords().size())
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            CorrelationIdUtil.setCorrelationId(event.getCorrelationId());
            logger.info("Processing inventory received with correlationId: {}, eventId: {}", 
                       event.getCorrelationId(), eventId);

            // Idempotency check
            if (eventProcessingService.isEventProcessed(eventId)) {
                logger.info("Event {} already processed, skipping (idempotency)", eventId);
                span.setAttribute("event.skipped", true);
                return;
            }

            try {
                // Log event
                String eventPayload = objectMapper.writeValueAsString(event);
                eventProcessingService.logEvent(
                    eventId,
                    event.getCorrelationId(),
                    "InventoryReceivedEvent",
                    EVENT_VERSION,
                    "inventory-service",
                    eventPayload
                );

                // Process each inventory item
                for (InventoryReceivedEvent.InventoryReceiveRecord record : event.getReceiveRecords()) {
                    InventoryItem item = inventoryItemMapper.findById(record.getInventoryItemId());
                    
                    if (item == null) {
                        // Create new item with metadata from event
                        item = new InventoryItem();
                        item.setId(record.getInventoryItemId());
                        item.setName(record.getName() != null ? record.getName() : "Item " + record.getInventoryItemId());
                        item.setDescription(record.getDescription() != null ? record.getDescription() : "Auto-created item");
                        item.setCategory(record.getCategory() != null ? record.getCategory() : "General");
                        item.setAvailableQuantity(0);
                        item.setCreatedAt(LocalDateTime.now());
                        item.setUpdatedAt(LocalDateTime.now());
                    } else {
                        // Update metadata if provided in event (for existing items)
                        if (record.getName() != null) {
                            item.setName(record.getName());
                        }
                        if (record.getDescription() != null) {
                            item.setDescription(record.getDescription());
                        }
                        if (record.getCategory() != null) {
                            item.setCategory(record.getCategory());
                        }
                    }
                    
                    item.setAvailableQuantity(item.getAvailableQuantity() + record.getQuantity());
                    item.setUpdatedAt(LocalDateTime.now());
                    
                    if (inventoryItemMapper.existsById(record.getInventoryItemId())) {
                        inventoryItemMapper.update(item);
                    } else {
                        inventoryItemMapper.insert(item);
                    }
                    
                    logger.info("Updated inventory item {} with quantity {} (name: {}, category: {})", 
                               record.getInventoryItemId(), item.getAvailableQuantity(), item.getName(), item.getCategory());
                    inventoryUpdatesCounter.add(1);
                }
                
                // Mark event as processed
                eventProcessingService.markEventAsProcessed(
                    eventId,
                    event.getCorrelationId(),
                    "InventoryReceivedEvent"
                );
                eventProcessingService.markEventLogAsProcessed(eventId);
                
                // In a full choreography pattern, we might publish an "inventory-persisted" event
                // for other services to react to. For now, we'll keep it simple.
                logger.info("Inventory persisted successfully for correlationId: {}", event.getCorrelationId());
                
                // Metrics
                long processingTime = System.currentTimeMillis() - startTime;
                eventsProcessedCounter.add(1, Attributes.of(
                        io.opentelemetry.api.common.AttributeKey.stringKey("event.type"), "InventoryReceivedEvent",
                        io.opentelemetry.api.common.AttributeKey.stringKey("status"), "success"
                ));
                span.setAttribute("inventory.status", "updated");
                span.setAttribute("inventory.items.updated", event.getReceiveRecords().size());
                span.setAttribute("processing.time.ms", processingTime);
                
            } catch (Exception e) {
                logger.error("Error processing inventory received for eventId: {}", eventId, e);
                eventsFailedCounter.add(1, Attributes.of(
                        io.opentelemetry.api.common.AttributeKey.stringKey("event.type"), "InventoryReceivedEvent",
                        io.opentelemetry.api.common.AttributeKey.stringKey("error.type"), e.getClass().getSimpleName()
                ));
                span.recordException(e);
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
                throw new RuntimeException("Failed to process inventory received", e);
            }
        } finally {
            span.end();
        }
    }

    /**
     * Counts confirmed reservations for a specific inventory item in a date range.
     * Used for calculating effective availability.
     * @deprecated Use sumReservationQuantitiesForItemInDateRange instead
     */
    @Deprecated
    public long countReservationsForItemInDateRange(String itemId, LocalDateTime startDate, LocalDateTime endDate) {
        return reservationMapper.countConfirmedReservationsForItemInDateRange(itemId, startDate, endDate);
    }

    /**
     * Sums quantities from confirmed reservations for a specific inventory item in a date range.
     * Used for calculating effective availability with quantities.
     */
    public long sumReservationQuantitiesForItemInDateRange(String itemId, LocalDateTime startDate, LocalDateTime endDate) {
        return reservationMapper.sumConfirmedReservationQuantitiesForItemInDateRange(itemId, startDate, endDate);
    }

    /**
     * Checks inventory availability for the requested items with quantities on the specified date.
     * Returns null if all items are available, or a reason string if unavailable.
     */
    private String checkInventoryAvailability(Map<String, Integer> inventoryItemQuantities, LocalDateTime reservationDate) {
        for (Map.Entry<String, Integer> entry : inventoryItemQuantities.entrySet()) {
            String itemId = entry.getKey();
            int requestedQuantity = entry.getValue();
            
            // Get inventory item
            InventoryItem item = inventoryItemMapper.findById(itemId);
            if (item == null) {
                return "Inventory item not found: " + itemId;
            }
            
            // Sum quantities from existing confirmed reservations for this item on this date
            long reservedQuantity = reservationMapper.sumConfirmedReservationQuantitiesForItemOnDate(itemId, reservationDate);
            
            // Check if there's availability (availableQuantity - reservedQuantity >= requestedQuantity)
            long availableCount = item.getAvailableQuantity() - reservedQuantity;
            if (availableCount < requestedQuantity) {
                return String.format("Insufficient availability for item %s on %s. Available: %d, Reserved: %d, Requested: %d", 
                        itemId, reservationDate, item.getAvailableQuantity(), reservedQuantity, requestedQuantity);
            }
        }
        return null; // All items are available
    }

    /**
     * Publishes an event to Kafka.
     * In production, this should use transactional outbox pattern for guaranteed delivery.
     */
    private void publishEvent(String topic, String key, Object event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, eventJson);
            logger.debug("Published event to topic: {}, key: {}", topic, key);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing event for topic: {}", topic, e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
}
