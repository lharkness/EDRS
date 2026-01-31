package com.edrs.logging.listener;

import com.edrs.common.events.CancellationRequestedEvent;
import com.edrs.common.events.CancellationSuccessfulEvent;
import com.edrs.common.events.InventoryReceivedEvent;
import com.edrs.common.events.ReservationCreatedEvent;
import com.edrs.common.events.ReservationRequestedEvent;
import com.edrs.common.util.CorrelationIdUtil;
import com.edrs.logging.service.EventLoggingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventLoggingListener {
    private static final Logger logger = LoggerFactory.getLogger(EventLoggingListener.class);

    private final EventLoggingService eventLoggingService;
    private final ObjectMapper objectMapper;

    public EventLoggingListener(EventLoggingService eventLoggingService, ObjectMapper objectMapper) {
        this.eventLoggingService = eventLoggingService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "reservation-requested", groupId = "logging-service-group")
    public void handleReservationRequested(String message) {
        try {
            ReservationRequestedEvent event = objectMapper.readValue(message, ReservationRequestedEvent.class);
            CorrelationIdUtil.setCorrelationId(event.getCorrelationId());
            eventLoggingService.logEvent("reservation-requested", "ReservationRequestedEvent", event, event.getCorrelationId());
        } catch (Exception e) {
            logger.error("Error logging reservation requested event", e);
        }
    }

    @KafkaListener(topics = "cancellation-requested", groupId = "logging-service-group")
    public void handleCancellationRequested(String message) {
        try {
            CancellationRequestedEvent event = objectMapper.readValue(message, CancellationRequestedEvent.class);
            CorrelationIdUtil.setCorrelationId(event.getCorrelationId());
            eventLoggingService.logEvent("cancellation-requested", "CancellationRequestedEvent", event, event.getCorrelationId());
        } catch (Exception e) {
            logger.error("Error logging cancellation requested event", e);
        }
    }

    @KafkaListener(topics = "reservation-created", groupId = "logging-service-group")
    public void handleReservationCreated(String message) {
        try {
            ReservationCreatedEvent event = objectMapper.readValue(message, ReservationCreatedEvent.class);
            CorrelationIdUtil.setCorrelationId(event.getCorrelationId());
            eventLoggingService.logEvent("reservation-created", "ReservationCreatedEvent", event, event.getCorrelationId());
        } catch (Exception e) {
            logger.error("Error logging reservation created event", e);
        }
    }

    @KafkaListener(topics = "cancellation-successful", groupId = "logging-service-group")
    public void handleCancellationSuccessful(String message) {
        try {
            CancellationSuccessfulEvent event = objectMapper.readValue(message, CancellationSuccessfulEvent.class);
            CorrelationIdUtil.setCorrelationId(event.getCorrelationId());
            eventLoggingService.logEvent("cancellation-successful", "CancellationSuccessfulEvent", event, event.getCorrelationId());
        } catch (Exception e) {
            logger.error("Error logging cancellation successful event", e);
        }
    }

    @KafkaListener(topics = "inventory-received", groupId = "logging-service-group")
    public void handleInventoryReceived(String message) {
        try {
            InventoryReceivedEvent event = objectMapper.readValue(message, InventoryReceivedEvent.class);
            CorrelationIdUtil.setCorrelationId(event.getCorrelationId());
            eventLoggingService.logEvent("inventory-received", "InventoryReceivedEvent", event, event.getCorrelationId());
        } catch (Exception e) {
            logger.error("Error logging inventory received event", e);
        }
    }
}
