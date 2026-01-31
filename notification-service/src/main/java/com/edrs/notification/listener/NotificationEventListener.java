package com.edrs.notification.listener;

import com.edrs.common.events.CancellationSuccessfulEvent;
import com.edrs.common.events.ReservationCreatedEvent;
import com.edrs.common.util.CorrelationIdUtil;
import com.edrs.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {
    private static final Logger logger = LoggerFactory.getLogger(NotificationEventListener.class);
    private static final String RESERVATION_CREATED_TOPIC = "reservation-created";
    private static final String CANCELLATION_SUCCESSFUL_TOPIC = "cancellation-successful";

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public NotificationEventListener(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = RESERVATION_CREATED_TOPIC, groupId = "notification-service-group")
    public void handleReservationCreated(String message) {
        try {
            CorrelationIdUtil.setCorrelationId(null); // Will be set from event
            ReservationCreatedEvent event = objectMapper.readValue(message, ReservationCreatedEvent.class);
            CorrelationIdUtil.setCorrelationId(event.getCorrelationId());
            
            logger.info("Received reservation created event with correlationId: {}", event.getCorrelationId());
            notificationService.sendReservationConfirmation(event.getUserId(), event.getConfirmationNumber());
        } catch (Exception e) {
            logger.error("Error processing reservation created event", e);
        }
    }

    @KafkaListener(topics = CANCELLATION_SUCCESSFUL_TOPIC, groupId = "notification-service-group")
    public void handleCancellationSuccessful(String message) {
        try {
            CorrelationIdUtil.setCorrelationId(null); // Will be set from event
            CancellationSuccessfulEvent event = objectMapper.readValue(message, CancellationSuccessfulEvent.class);
            CorrelationIdUtil.setCorrelationId(event.getCorrelationId());
            
            logger.info("Received cancellation successful event with correlationId: {}", event.getCorrelationId());
            notificationService.sendCancellationConfirmation(event.getUserId(), event.getConfirmationNumber());
        } catch (Exception e) {
            logger.error("Error processing cancellation successful event", e);
        }
    }
}
