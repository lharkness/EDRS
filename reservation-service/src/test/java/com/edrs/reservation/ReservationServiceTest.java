package com.edrs.reservation;

import com.edrs.reservation.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    classes = ReservationServiceApplication.class
)
@TestPropertySource(properties = {
    "opentelemetry.enabled=false",
    "spring.kafka.bootstrap-servers=localhost:9092",
    "spring.kafka.consumer.auto-offset-reset=earliest"
})
public class ReservationServiceTest {
    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired(required = false)
    private ReservationService reservationService;

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    @Test
    public void testMakeReservation() {
        // Verify that the service was autowired
        org.junit.jupiter.api.Assertions.assertNotNull(reservationService, 
            "ReservationService should be autowired. Check if Spring context loaded properly.");
        org.junit.jupiter.api.Assertions.assertNotNull(objectMapper, 
            "ObjectMapper should be autowired.");
        
        String userId = "user123";
        var inventoryItemIds = Arrays.asList("item1", "item2");
        LocalDateTime reservationDate = LocalDateTime.now().plusDays(1);

        reservationService.makeReservation(userId, inventoryItemIds, reservationDate);

        verify(kafkaTemplate).send(eq("reservation-requested"), any(String.class), any(String.class));
    }
}
