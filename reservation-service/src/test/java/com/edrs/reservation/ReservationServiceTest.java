package com.edrs.reservation;

import com.edrs.reservation.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ReservationServiceTest {

    @Test
    public void testMakeReservation() {
        // Initialize dependencies directly in test
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        ReservationService reservationService = new ReservationService(kafkaTemplate, objectMapper);
        
        String userId = "user123";
        var inventoryItemIds = Arrays.asList("item1", "item2");
        LocalDateTime reservationDate = LocalDateTime.now().plusDays(1);

        reservationService.makeReservation(userId, inventoryItemIds, reservationDate);

        verify(kafkaTemplate).send(eq("reservation-requested"), any(String.class), any(String.class));
    }
}
