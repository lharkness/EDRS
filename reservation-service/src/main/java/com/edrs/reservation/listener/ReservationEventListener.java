package com.edrs.reservation.listener;

import com.edrs.common.events.CancellationSuccessfulEvent;
import com.edrs.common.events.ReservationCreatedEvent;
import com.edrs.common.util.CorrelationIdUtil;
import com.edrs.reservation.dto.ReservationResponse;
import com.edrs.reservation.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReservationEventListener {
    private static final Logger logger = LoggerFactory.getLogger(ReservationEventListener.class);
    private static final String RESERVATION_CREATED_TOPIC = "reservation-created";
    private static final String CANCELLATION_SUCCESSFUL_TOPIC = "cancellation-successful";

    private final ReservationService reservationService;
    private final ObjectMapper objectMapper;

    public ReservationEventListener(ReservationService reservationService, ObjectMapper objectMapper) {
        this.reservationService = reservationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = RESERVATION_CREATED_TOPIC, groupId = "reservation-service-group")
    public void handleReservationCreated(String message) {
        try {
            CorrelationIdUtil.setCorrelationId(null); // Will be set from event
            ReservationCreatedEvent event = objectMapper.readValue(message, ReservationCreatedEvent.class);
            CorrelationIdUtil.setCorrelationId(event.getCorrelationId());
            
            logger.info("Received reservation created event with correlationId: {}", event.getCorrelationId());
            
            ReservationResponse response = new ReservationResponse();
            response.setConfirmationNumber(event.getConfirmationNumber());
            response.setUserId(event.getUserId());
            response.setInventoryItemIds(event.getInventoryItemIds());
            response.setReservationDate(event.getReservationDate());
            response.setStatus("CONFIRMED");
            
            reservationService.updateReservation(response);
            logger.info("Updated reservation store with confirmation number: {}", event.getConfirmationNumber());
        } catch (Exception e) {
            logger.error("Error processing reservation created event", e);
        }
    }

    @KafkaListener(topics = CANCELLATION_SUCCESSFUL_TOPIC, groupId = "reservation-service-group")
    public void handleCancellationSuccessful(String message) {
        try {
            CorrelationIdUtil.setCorrelationId(null); // Will be set from event
            CancellationSuccessfulEvent event = objectMapper.readValue(message, CancellationSuccessfulEvent.class);
            CorrelationIdUtil.setCorrelationId(event.getCorrelationId());
            
            logger.info("Received cancellation successful event with correlationId: {}", event.getCorrelationId());
            
            ReservationResponse reservation = reservationService.getReservation(event.getConfirmationNumber());
            if (reservation != null) {
                reservation.setStatus("CANCELLED");
                reservationService.updateReservation(reservation);
                logger.info("Updated reservation status to CANCELLED for: {}", event.getConfirmationNumber());
            }
        } catch (Exception e) {
            logger.error("Error processing cancellation successful event", e);
        }
    }
}
