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

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@TestPropertySource(properties = {
    "opentelemetry.enabled=false"
})
public class ReservationServiceTest {
    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testMakeReservation() {
        String userId = "user123";
        var inventoryItemIds = Arrays.asList("item1", "item2");
        LocalDateTime reservationDate = LocalDateTime.now().plusDays(1);

        reservationService.makeReservation(userId, inventoryItemIds, reservationDate);

        verify(kafkaTemplate).send(eq("reservation-requested"), any(String.class), any(String.class));
    }
}
