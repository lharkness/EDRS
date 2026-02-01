package com.edrs.reservation.service;

import com.edrs.common.events.CancellationRequestedEvent;
import com.edrs.common.events.ReservationRequestedEvent;
import com.edrs.common.util.CorrelationIdUtil;
import com.edrs.reservation.dto.ReservationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ReservationService {
    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);
    private static final String RESERVATION_REQUESTED_TOPIC = "reservation-requested";
    private static final String CANCELLATION_REQUESTED_TOPIC = "cancellation-requested";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, ReservationResponse> reservationStore = new HashMap<>();

    @Autowired
    public ReservationService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public String makeReservation(String userId, java.util.Map<String, Integer> inventoryItemQuantities, LocalDateTime reservationDate) {
        UUID correlationId = CorrelationIdUtil.generateCorrelationId();
        logger.info("Making reservation request with correlationId: {}, items: {}", correlationId, inventoryItemQuantities);

        ReservationRequestedEvent event = new ReservationRequestedEvent(
                correlationId,
                userId,
                inventoryItemQuantities,
                reservationDate,
                LocalDateTime.now()
        );

        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(RESERVATION_REQUESTED_TOPIC, correlationId.toString(), eventJson);
            logger.info("Published reservation requested event with correlationId: {}", correlationId);
            return correlationId.toString();
        } catch (JsonProcessingException e) {
            logger.error("Error serializing reservation requested event", e);
            throw new RuntimeException("Failed to create reservation request", e);
        }
    }

    public void cancelReservation(String confirmationNumber) {
        UUID correlationId = CorrelationIdUtil.generateCorrelationId();
        logger.info("Cancelling reservation {} with correlationId: {}", confirmationNumber, correlationId);

        CancellationRequestedEvent event = new CancellationRequestedEvent(
                correlationId,
                confirmationNumber,
                LocalDateTime.now()
        );

        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(CANCELLATION_REQUESTED_TOPIC, correlationId.toString(), eventJson);
            logger.info("Published cancellation requested event with correlationId: {}", correlationId);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing cancellation requested event", e);
            throw new RuntimeException("Failed to create cancellation request", e);
        }
    }

    public ReservationResponse getReservation(String confirmationNumber) {
        return reservationStore.get(confirmationNumber);
    }

    public void updateReservation(ReservationResponse reservation) {
        reservationStore.put(reservation.getConfirmationNumber(), reservation);
    }

    public java.util.List<ReservationResponse> listReservations(String userId) {
        if (userId != null && !userId.isEmpty()) {
            return reservationStore.values().stream()
                    .filter(reservation -> userId.equals(reservation.getUserId()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return new java.util.ArrayList<>(reservationStore.values());
    }
}
