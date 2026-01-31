package com.edrs.persistence.service;

import com.edrs.persistence.entity.EventLog;
import com.edrs.persistence.entity.ProcessedEvent;
import com.edrs.persistence.mapper.EventLogMapper;
import com.edrs.persistence.mapper.ProcessedEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventProcessingServiceTest {

    @Mock
    private ProcessedEventMapper processedEventMapper;

    @Mock
    private EventLogMapper eventLogMapper;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private EventProcessingService eventProcessingService;

    private UUID eventId;
    private UUID correlationId;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        correlationId = UUID.randomUUID();
    }

    @Test
    void testIsEventProcessed_ReturnsTrue() {
        // Given
        when(processedEventMapper.existsByEventId(eventId)).thenReturn(true);

        // When
        boolean result = eventProcessingService.isEventProcessed(eventId);

        // Then
        assertTrue(result);
        verify(processedEventMapper).existsByEventId(eventId);
    }

    @Test
    void testIsEventProcessed_ReturnsFalse() {
        // Given
        when(processedEventMapper.existsByEventId(eventId)).thenReturn(false);

        // When
        boolean result = eventProcessingService.isEventProcessed(eventId);

        // Then
        assertFalse(result);
        verify(processedEventMapper).existsByEventId(eventId);
    }

    @Test
    void testMarkEventAsProcessed_NewEvent() {
        // Given
        String eventType = "ReservationRequestedEvent";
        when(processedEventMapper.existsByEventId(eventId)).thenReturn(false);
        doNothing().when(processedEventMapper).insert(any(ProcessedEvent.class));

        // When
        eventProcessingService.markEventAsProcessed(eventId, correlationId, eventType);

        // Then
        ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventMapper).insert(captor.capture());
        
        ProcessedEvent saved = captor.getValue();
        assertEquals(eventId, saved.getEventId());
        assertEquals(correlationId, saved.getCorrelationId());
        assertEquals(eventType, saved.getEventType());
        assertEquals("persistence-service", saved.getHandlerService());
        assertNotNull(saved.getProcessedAt());
    }

    @Test
    void testMarkEventAsProcessed_AlreadyProcessed() {
        // Given
        String eventType = "ReservationRequestedEvent";
        when(processedEventMapper.existsByEventId(eventId)).thenReturn(true);

        // When
        eventProcessingService.markEventAsProcessed(eventId, correlationId, eventType);

        // Then
        verify(processedEventMapper).existsByEventId(eventId);
        verify(processedEventMapper, never()).insert(any(ProcessedEvent.class));
    }

    @Test
    void testLogEvent_Success() {
        // Given
        String eventType = "ReservationRequestedEvent";
        String eventVersion = "1.0";
        String source = "reservation-service";
        String payload = "{\"test\":\"data\"}";
        
        doNothing().when(eventLogMapper).insert(any(EventLog.class));

        // When
        eventProcessingService.logEvent(eventId, correlationId, eventType, eventVersion, source, payload);

        // Then
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventLogMapper).insert(captor.capture());
        
        EventLog saved = captor.getValue();
        assertEquals(eventId, saved.getEventId());
        assertEquals(correlationId, saved.getCorrelationId());
        assertEquals(eventType, saved.getEventType());
        assertEquals(eventVersion, saved.getEventVersion());
        assertEquals(source, saved.getSource());
        assertEquals(payload, saved.getPayload());
        assertFalse(saved.getProcessed());
        assertNotNull(saved.getTimestamp());
    }

    @Test
    void testLogEvent_ExceptionHandled() {
        // Given
        String eventType = "ReservationRequestedEvent";
        String eventVersion = "1.0";
        String source = "reservation-service";
        String payload = "{\"test\":\"data\"}";
        
        doThrow(new RuntimeException("DB error")).when(eventLogMapper).insert(any(EventLog.class));

        // When - should not throw
        assertDoesNotThrow(() -> 
                eventProcessingService.logEvent(eventId, correlationId, eventType, eventVersion, source, payload));

        // Then
        verify(eventLogMapper).insert(any(EventLog.class));
    }

    @Test
    void testMarkEventLogAsProcessed_EventLogExists() {
        // Given
        EventLog eventLog = new EventLog();
        eventLog.setEventId(eventId);
        eventLog.setProcessed(false);
        
        when(eventLogMapper.findByEventId(eventId)).thenReturn(Optional.of(eventLog));
        doNothing().when(eventLogMapper).updateProcessed(eq(eventId), any(LocalDateTime.class));

        // When
        eventProcessingService.markEventLogAsProcessed(eventId);

        // Then
        verify(eventLogMapper).findByEventId(eventId);
        verify(eventLogMapper).updateProcessed(eq(eventId), any(LocalDateTime.class));
    }

    @Test
    void testMarkEventLogAsProcessed_EventLogNotFound() {
        // Given
        when(eventLogMapper.findByEventId(eventId)).thenReturn(Optional.empty());

        // When
        eventProcessingService.markEventLogAsProcessed(eventId);

        // Then
        verify(eventLogMapper).findByEventId(eventId);
        verify(eventLogMapper, never()).updateProcessed(any(), any());
    }

    @Test
    void testGenerateEventId() {
        // When
        UUID generatedId1 = eventProcessingService.generateEventId();
        UUID generatedId2 = eventProcessingService.generateEventId();

        // Then
        assertNotNull(generatedId1);
        assertNotNull(generatedId2);
        assertNotEquals(generatedId1, generatedId2); // Should be unique
    }
}
