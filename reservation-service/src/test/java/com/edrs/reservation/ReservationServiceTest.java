package com.edrs.reservation;

import com.edrs.reservation.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    classes = {ReservationServiceTest.TestConfig.class, ReservationServiceApplication.class},
    properties = {
        "opentelemetry.enabled=false"
    }
)
@ComponentScan(
    basePackages = "com.edrs.reservation",
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                com.edrs.reservation.listener.ReservationEventListener.class,
                com.edrs.reservation.config.KafkaConfig.class
            }
        )
    }
)
@TestPropertySource(properties = {
    "opentelemetry.enabled=false"
})
public class ReservationServiceTest {
    
    @TestConfiguration
    static class TestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper;
        }
    }
    
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
