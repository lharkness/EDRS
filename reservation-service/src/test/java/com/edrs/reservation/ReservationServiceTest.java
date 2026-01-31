package com.edrs.reservation;

import com.edrs.reservation.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceTest {
    
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;
    private ReservationService reservationService;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        reservationService = new ReservationService(kafkaTemplate, objectMapper);
    }

    @Test
    public void testMakeReservation() {
        String userId = "user123";
        var inventoryItemIds = Arrays.asList("item1", "item2");
        LocalDateTime reservationDate = LocalDateTime.now().plusDays(1);

        reservationService.makeReservation(userId, inventoryItemIds, reservationDate);

        verify(kafkaTemplate).send(eq("reservation-requested"), any(String.class), any(String.class));
    }
}
