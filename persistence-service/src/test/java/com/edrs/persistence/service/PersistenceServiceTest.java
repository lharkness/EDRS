package com.edrs.persistence.service;

import com.edrs.common.events.CancellationRequestedEvent;
import com.edrs.common.events.CancellationSuccessfulEvent;
import com.edrs.common.events.InventoryReceivedEvent;
import com.edrs.common.events.ReservationCreatedEvent;
import com.edrs.common.events.ReservationFailedEvent;
import com.edrs.common.events.ReservationRequestedEvent;
import com.edrs.persistence.entity.InventoryItem;
import com.edrs.persistence.entity.Reservation;
import com.edrs.persistence.mapper.InventoryItemMapper;
import com.edrs.persistence.mapper.ReservationMapper;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongCounterBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersistenceServiceTest {

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private InventoryItemMapper inventoryItemMapper;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EventProcessingService eventProcessingService;

    @Mock
    private Tracer tracer;

    @Mock
    private Meter meter;

    private PersistenceService persistenceService;

    private UUID correlationId;
    private UUID eventId;
    private LocalDateTime reservationDate;
    
    // OpenTelemetry mocks - declared as fields so they can be used in tests
    private Context mockContext;

    @BeforeEach
    void setUp() {
        correlationId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        reservationDate = LocalDateTime.now().plusDays(1);
        
        // Initialize OpenTelemetry mocks
        mockContext = mock(Context.class);
        Span mockSpan = mock(Span.class);
        Span mockChildSpan = mock(Span.class);
        SpanBuilder mockSpanBuilder = mock(SpanBuilder.class);
        SpanBuilder mockChildSpanBuilder = mock(SpanBuilder.class);
        Scope mockScope = mock(Scope.class);
        Scope mockChildScope = mock(Scope.class);
        
        // Return different builders based on span name
        when(tracer.spanBuilder(anyString())).thenAnswer(invocation -> {
            String spanName = invocation.getArgument(0);
            if ("checkInventoryAvailability".equals(spanName)) {
                return mockChildSpanBuilder;
            }
            return mockSpanBuilder;
        });
        
        // Mock child span builder (for availability check)
        lenient().when(mockChildSpanBuilder.setAttribute(anyString(), anyString())).thenReturn(mockChildSpanBuilder);
        lenient().when(mockChildSpanBuilder.setAttribute(anyString(), anyBoolean())).thenReturn(mockChildSpanBuilder);
        lenient().when(mockChildSpanBuilder.setParent(any())).thenReturn(mockChildSpanBuilder);
        lenient().when(mockChildSpanBuilder.startSpan()).thenReturn(mockChildSpan);
        lenient().when(mockChildSpan.makeCurrent()).thenReturn(mockChildScope);
        lenient().when(mockChildSpan.setAttribute(anyString(), anyString())).thenReturn(mockChildSpan);
        lenient().when(mockChildSpan.setAttribute(anyString(), anyBoolean())).thenReturn(mockChildSpan);
        lenient().doNothing().when(mockChildSpan).end();
        // Mock setAttribute for String key with various value types
        lenient().when(mockSpanBuilder.setAttribute(anyString(), anyString())).thenReturn(mockSpanBuilder);
        lenient().when(mockSpanBuilder.setAttribute(anyString(), anyLong())).thenReturn(mockSpanBuilder);
        lenient().when(mockSpanBuilder.setAttribute(anyString(), anyInt())).thenReturn(mockSpanBuilder);
        lenient().when(mockSpanBuilder.setAttribute(anyString(), anyBoolean())).thenReturn(mockSpanBuilder);
        lenient().when(mockSpanBuilder.setParent(any())).thenReturn(mockSpanBuilder);
        when(mockSpanBuilder.startSpan()).thenReturn(mockSpan);
        when(mockSpan.makeCurrent()).thenReturn(mockScope);
        lenient().when(mockSpan.setAttribute(anyString(), anyString())).thenReturn(mockSpan);
        lenient().when(mockSpan.setAttribute(anyString(), anyLong())).thenReturn(mockSpan);
        lenient().when(mockSpan.setAttribute(anyString(), anyInt())).thenReturn(mockSpan);
        lenient().when(mockSpan.setAttribute(anyString(), anyBoolean())).thenReturn(mockSpan);
        lenient().when(mockSpan.setStatus(any())).thenReturn(mockSpan);
        lenient().when(mockSpan.recordException(any())).thenReturn(mockSpan);
        
        // Mock OpenTelemetry Meter and Counter Builders
        LongCounter mockCounter = mock(LongCounter.class);
        lenient().doNothing().when(mockCounter).add(anyLong(), any());
        lenient().doNothing().when(mockCounter).add(anyLong());
        
        // Use Answer to chain the builder methods
        LongCounterBuilder mockCounterBuilder = mock(LongCounterBuilder.class);
        when(meter.counterBuilder(anyString())).thenReturn(mockCounterBuilder);
        when(mockCounterBuilder.setDescription(anyString())).thenReturn(mockCounterBuilder);
        when(mockCounterBuilder.build()).thenReturn(mockCounter);
        
        // Manually construct service after mocks are set up
        persistenceService = new PersistenceService(
                reservationMapper,
                inventoryItemMapper,
                kafkaTemplate,
                objectMapper,
                eventProcessingService,
                tracer,
                meter
        );
    }

    @Test
    void testProcessReservationRequest_Success() throws JsonProcessingException {
        // Mock static Context.current() for this test
        try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
            contextMock.when(() -> Context.current()).thenReturn(mockContext);
            lenient().when(mockContext.with(any(Span.class))).thenReturn(mockContext);
            
            // Given
        String userId = "user123";
        Map<String, Integer> inventoryItemQuantities = new HashMap<>();
        inventoryItemQuantities.put("item1", 2);
        inventoryItemQuantities.put("item2", 1);
        ReservationRequestedEvent event = new ReservationRequestedEvent(
                correlationId, userId, inventoryItemQuantities, reservationDate, LocalDateTime.now());

        // Mock inventory items with available quantity
        InventoryItem item1 = new InventoryItem();
        item1.setId("item1");
        item1.setAvailableQuantity(10);
        InventoryItem item2 = new InventoryItem();
        item2.setId("item2");
        item2.setAvailableQuantity(5);

        String eventPayload = "{\"correlationId\":\"" + correlationId + "\"}";
        when(eventProcessingService.isEventProcessed(eventId)).thenReturn(false);
        when(objectMapper.writeValueAsString(event)).thenReturn(eventPayload);
        when(inventoryItemMapper.findById("item1")).thenReturn(item1);
        when(inventoryItemMapper.findById("item2")).thenReturn(item2);
        when(reservationMapper.sumConfirmedReservationQuantitiesForItemOnDate("item1", reservationDate)).thenReturn(0L);
        when(reservationMapper.sumConfirmedReservationQuantitiesForItemOnDate("item2", reservationDate)).thenReturn(0L);
        doNothing().when(reservationMapper).insert(any(Reservation.class));
        doNothing().when(reservationMapper).insertReservationItem(anyString(), anyString(), anyInt());

        @SuppressWarnings("unchecked")
        SendResult<String, String> mockSendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mockSendResult);
        lenient().when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        // When
        persistenceService.processReservationRequest(event, eventId);

        // Then
        verify(eventProcessingService).isEventProcessed(eventId);
        verify(eventProcessingService).logEvent(
                eq(eventId),
                eq(correlationId),
                eq("ReservationRequestedEvent"),
                eq("1.0"),
                eq("reservation-service"),
                eq(eventPayload)
        );
        ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationMapper).insert(reservationCaptor.capture());
        Reservation savedReservation = reservationCaptor.getValue();
        assertEquals(userId, savedReservation.getUserId());
        assertEquals("CONFIRMED", savedReservation.getStatus());
        verify(reservationMapper, times(inventoryItemQuantities.size())).insertReservationItem(eq(savedReservation.getConfirmationNumber()), anyString(), anyInt());
        verify(eventProcessingService).markEventAsProcessed(eventId, correlationId, "ReservationRequestedEvent");
        verify(eventProcessingService).markEventLogAsProcessed(eventId);
        
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());
        assertEquals("reservation-created", topicCaptor.getValue());
        assertEquals(correlationId.toString(), keyCaptor.getValue());
        }
    }

    @Test
    void testProcessReservationRequest_Idempotency() {
        // Mock static Context.current() for this test
        try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
            contextMock.when(() -> Context.current()).thenReturn(mockContext);
            lenient().when(mockContext.with(any(Span.class))).thenReturn(mockContext);
            
            // Given
            Map<String, Integer> itemQuantities = new HashMap<>();
            itemQuantities.put("item1", 1);
            ReservationRequestedEvent event = new ReservationRequestedEvent(
                    correlationId, "user123", itemQuantities, reservationDate, LocalDateTime.now());
            when(eventProcessingService.isEventProcessed(eventId)).thenReturn(true);

            // When
            persistenceService.processReservationRequest(event, eventId);

            // Then
            verify(eventProcessingService).isEventProcessed(eventId);
            verify(eventProcessingService, never()).logEvent(any(), any(), any(), any(), any(), any());
            verify(reservationMapper, never()).insert(any(Reservation.class));
            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }
    }

    @Test
    void testProcessReservationRequest_JsonProcessingException() throws JsonProcessingException {
        // Mock static Context.current() for this test
        try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
            contextMock.when(() -> Context.current()).thenReturn(mockContext);
            lenient().when(mockContext.with(any(Span.class))).thenReturn(mockContext);
            
            // Given
            Map<String, Integer> itemQuantities = new HashMap<>();
            itemQuantities.put("item1", 1);
            ReservationRequestedEvent event = new ReservationRequestedEvent(
                    correlationId, "user123", itemQuantities, reservationDate, LocalDateTime.now());
            when(eventProcessingService.isEventProcessed(eventId)).thenReturn(false);
            when(objectMapper.writeValueAsString(event)).thenThrow(new JsonProcessingException("JSON error") {});

            // When/Then
            assertThrows(RuntimeException.class, () -> 
                    persistenceService.processReservationRequest(event, eventId));
            
            verify(eventProcessingService).isEventProcessed(eventId);
            verify(reservationMapper, never()).insert(any(Reservation.class));
        }
    }

    @Test
    void testProcessCancellationRequest_Success() throws JsonProcessingException {
        // Mock static Context.current() for this test
        try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
            contextMock.when(() -> Context.current()).thenReturn(mockContext);
            lenient().when(mockContext.with(any(Span.class))).thenReturn(mockContext);
        // Given
        String confirmationNumber = "CONF-123";
        CancellationRequestedEvent event = new CancellationRequestedEvent(
                correlationId, confirmationNumber, LocalDateTime.now());

        Reservation reservation = new Reservation();
        reservation.setConfirmationNumber(confirmationNumber);
        reservation.setUserId("user123");
        reservation.setStatus("CONFIRMED");

        String eventPayload = "{\"confirmationNumber\":\"" + confirmationNumber + "\"}";
        String cancellationEventJson = "{\"confirmationNumber\":\"" + confirmationNumber + "\"}";
        when(eventProcessingService.isEventProcessed(eventId)).thenReturn(false);
        when(objectMapper.writeValueAsString(event)).thenReturn(eventPayload);
        when(objectMapper.writeValueAsString(any(CancellationSuccessfulEvent.class))).thenReturn(cancellationEventJson);
        when(reservationMapper.findByConfirmationNumber(confirmationNumber)).thenReturn(reservation);
        doNothing().when(reservationMapper).update(any(Reservation.class));

        @SuppressWarnings("unchecked")
        SendResult<String, String> mockSendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mockSendResult);
        lenient().when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        // When
        persistenceService.processCancellationRequest(event, eventId);

        // Then
        verify(eventProcessingService).isEventProcessed(eventId);
        verify(eventProcessingService).logEvent(
                eq(eventId),
                eq(correlationId),
                eq("CancellationRequestedEvent"),
                eq("1.0"),
                eq("reservation-service"),
                eq(eventPayload)
        );
        verify(reservationMapper).findByConfirmationNumber(confirmationNumber);
        ArgumentCaptor<Reservation> updateCaptor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationMapper).update(updateCaptor.capture());
        assertEquals("CANCELLED", updateCaptor.getValue().getStatus());
        verify(eventProcessingService).markEventAsProcessed(eventId, correlationId, "CancellationRequestedEvent");
        verify(eventProcessingService).markEventLogAsProcessed(eventId);
        verify(kafkaTemplate).send(eq("cancellation-successful"), eq(correlationId.toString()), anyString());
        }
    }

    @Test
    void testProcessCancellationRequest_ReservationNotFound() throws JsonProcessingException {
        // Mock static Context.current() for this test
        try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
            contextMock.when(() -> Context.current()).thenReturn(mockContext);
            lenient().when(mockContext.with(any(Span.class))).thenReturn(mockContext);
            
            // Given
            String confirmationNumber = "CONF-999";
            CancellationRequestedEvent event = new CancellationRequestedEvent(
                    correlationId, confirmationNumber, LocalDateTime.now());

            String eventPayload = "{\"confirmationNumber\":\"" + confirmationNumber + "\"}";
            when(eventProcessingService.isEventProcessed(eventId)).thenReturn(false);
            when(objectMapper.writeValueAsString(event)).thenReturn(eventPayload);
            when(reservationMapper.findByConfirmationNumber(confirmationNumber)).thenReturn(null);

            // When/Then
            assertThrows(RuntimeException.class, () -> 
                    persistenceService.processCancellationRequest(event, eventId));
            
            verify(eventProcessingService).isEventProcessed(eventId);
            verify(reservationMapper).findByConfirmationNumber(confirmationNumber);
            verify(reservationMapper, never()).update(any(Reservation.class));
            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }
    }

    @Test
    void testProcessCancellationRequest_Idempotency() {
        // Mock static Context.current() for this test
        try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
            contextMock.when(() -> Context.current()).thenReturn(mockContext);
            lenient().when(mockContext.with(any(Span.class))).thenReturn(mockContext);
            
            // Given
            CancellationRequestedEvent event = new CancellationRequestedEvent(
                    correlationId, "CONF-123", LocalDateTime.now());
            when(eventProcessingService.isEventProcessed(eventId)).thenReturn(true);

            // When
            persistenceService.processCancellationRequest(event, eventId);

            // Then
            verify(eventProcessingService).isEventProcessed(eventId);
            verify(reservationMapper, never()).findByConfirmationNumber(anyString());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }
    }

    @Test
    void testProcessInventoryReceived_Success_NewItem() throws JsonProcessingException {
        // Mock static Context.current() for this test
        try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
            contextMock.when(() -> Context.current()).thenReturn(mockContext);
            lenient().when(mockContext.with(any(Span.class))).thenReturn(mockContext);
            
            // Given
            String itemId = "item1";
            int quantity = 10;
            InventoryReceivedEvent.InventoryReceiveRecord record = 
                    new InventoryReceivedEvent.InventoryReceiveRecord(itemId, quantity, "Test Item", "Test Description", "Test Category");
            InventoryReceivedEvent event = new InventoryReceivedEvent(
                    correlationId, Arrays.asList(record), LocalDateTime.now());

            String eventPayload = "{\"inventoryItemId\":\"" + itemId + "\"}";
            when(eventProcessingService.isEventProcessed(eventId)).thenReturn(false);
            when(objectMapper.writeValueAsString(event)).thenReturn(eventPayload);
            when(inventoryItemMapper.findById(itemId)).thenReturn(null);
            when(inventoryItemMapper.existsById(itemId)).thenReturn(false);
            doNothing().when(inventoryItemMapper).insert(any(InventoryItem.class));

            // When
            persistenceService.processInventoryReceived(event, eventId);

            // Then
            verify(eventProcessingService).isEventProcessed(eventId);
            verify(eventProcessingService).logEvent(
                    eq(eventId),
                    eq(correlationId),
                    eq("InventoryReceivedEvent"),
                    eq("1.0"),
                    eq("inventory-service"),
                    eq(eventPayload)
            );
            verify(inventoryItemMapper).findById(itemId);
            ArgumentCaptor<InventoryItem> itemCaptor = ArgumentCaptor.forClass(InventoryItem.class);
            verify(inventoryItemMapper).insert(itemCaptor.capture());
            InventoryItem savedItem = itemCaptor.getValue();
            assertEquals(itemId, savedItem.getId());
            assertEquals(quantity, savedItem.getAvailableQuantity());
            assertEquals("Test Item", savedItem.getName());
            verify(eventProcessingService).markEventAsProcessed(eventId, correlationId, "InventoryReceivedEvent");
            verify(eventProcessingService).markEventLogAsProcessed(eventId);
        }
    }

    @Test
    void testProcessInventoryReceived_Success_ExistingItem() throws JsonProcessingException {
        // Mock static Context.current() for this test
        try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
            contextMock.when(() -> Context.current()).thenReturn(mockContext);
            lenient().when(mockContext.with(any(Span.class))).thenReturn(mockContext);
            
            // Given
            String itemId = "item1";
            int existingQuantity = 5;
        int additionalQuantity = 10;
        
        InventoryItem existingItem = new InventoryItem();
        existingItem.setId(itemId);
        existingItem.setName("Laptop");
        existingItem.setAvailableQuantity(existingQuantity);

        InventoryReceivedEvent.InventoryReceiveRecord record = 
                new InventoryReceivedEvent.InventoryReceiveRecord(itemId, additionalQuantity, "Test Item", "Test Description", "Test Category");
        InventoryReceivedEvent event = new InventoryReceivedEvent(
                correlationId, Arrays.asList(record), LocalDateTime.now());

        String eventPayload = "{\"inventoryItemId\":\"" + itemId + "\"}";
        when(eventProcessingService.isEventProcessed(eventId)).thenReturn(false);
        when(objectMapper.writeValueAsString(event)).thenReturn(eventPayload);
        when(inventoryItemMapper.findById(itemId)).thenReturn(existingItem);
        when(inventoryItemMapper.existsById(itemId)).thenReturn(true);
        doNothing().when(inventoryItemMapper).update(any(InventoryItem.class));

        // When
        persistenceService.processInventoryReceived(event, eventId);

        // Then
        verify(inventoryItemMapper).findById(itemId);
        ArgumentCaptor<InventoryItem> updateCaptor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(inventoryItemMapper).update(updateCaptor.capture());
        assertEquals(existingQuantity + additionalQuantity, updateCaptor.getValue().getAvailableQuantity());
        }
    }

    @Test
    void testProcessInventoryReceived_MultipleItems() throws JsonProcessingException {
        // Mock static Context.current() for this test
        try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
            contextMock.when(() -> Context.current()).thenReturn(mockContext);
            lenient().when(mockContext.with(any(Span.class))).thenReturn(mockContext);
        // Given
        InventoryReceivedEvent.InventoryReceiveRecord record1 = 
                new InventoryReceivedEvent.InventoryReceiveRecord("item1", 10, "Item 1", "Description 1", "Category 1");
        InventoryReceivedEvent.InventoryReceiveRecord record2 = 
                new InventoryReceivedEvent.InventoryReceiveRecord("item2", 5, "Item 2", "Description 2", "Category 2");
        InventoryReceivedEvent event = new InventoryReceivedEvent(
                correlationId, Arrays.asList(record1, record2), LocalDateTime.now());

        String eventPayload = "{}";
        when(eventProcessingService.isEventProcessed(eventId)).thenReturn(false);
        when(objectMapper.writeValueAsString(event)).thenReturn(eventPayload);
        when(inventoryItemMapper.findById(anyString())).thenReturn(null);
        doNothing().when(inventoryItemMapper).insert(any(InventoryItem.class));

        // When
        persistenceService.processInventoryReceived(event, eventId);

        // Then
        verify(inventoryItemMapper, times(2)).findById(anyString());
        verify(inventoryItemMapper, times(2)).insert(any(InventoryItem.class));
        }
    }

    @Test
    void testProcessInventoryReceived_Idempotency() {
        // Mock static Context.current() for this test
        try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
            contextMock.when(() -> Context.current()).thenReturn(mockContext);
            lenient().when(mockContext.with(any(Span.class))).thenReturn(mockContext);
            
            // Given
            InventoryReceivedEvent.InventoryReceiveRecord record = 
                    new InventoryReceivedEvent.InventoryReceiveRecord("item1", 10, "Test Item", "Test Description", "Test Category");
            InventoryReceivedEvent event = new InventoryReceivedEvent(
                    correlationId, Arrays.asList(record), LocalDateTime.now());
            when(eventProcessingService.isEventProcessed(eventId)).thenReturn(true);

            // When
            persistenceService.processInventoryReceived(event, eventId);

            // Then
            verify(eventProcessingService).isEventProcessed(eventId);
            verify(inventoryItemMapper, never()).findById(anyString());
            verify(inventoryItemMapper, never()).insert(any(InventoryItem.class));
            verify(inventoryItemMapper, never()).update(any(InventoryItem.class));
        }
    }

    @Test
    void testProcessInventoryReceived_Exception() throws JsonProcessingException {
        // Mock static Context.current() for this test
        try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
            contextMock.when(() -> Context.current()).thenReturn(mockContext);
            lenient().when(mockContext.with(any(Span.class))).thenReturn(mockContext);
            
            // Given
            InventoryReceivedEvent.InventoryReceiveRecord record = 
                    new InventoryReceivedEvent.InventoryReceiveRecord("item1", 10, "Test Item", "Test Description", "Test Category");
            InventoryReceivedEvent event = new InventoryReceivedEvent(
                    correlationId, Arrays.asList(record), LocalDateTime.now());

            when(eventProcessingService.isEventProcessed(eventId)).thenReturn(false);
            when(objectMapper.writeValueAsString(event)).thenReturn("{}");
            when(inventoryItemMapper.findById(anyString())).thenThrow(new RuntimeException("DB error"));

            // When/Then
            assertThrows(RuntimeException.class, () -> 
                    persistenceService.processInventoryReceived(event, eventId));
        }
    }

    @Test
    void testProcessReservationRequest_UnavailabilityAfterFirstReservation() throws JsonProcessingException {
        // Mock static Context.current() for this test
        try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
            contextMock.when(() -> Context.current()).thenReturn(mockContext);
            lenient().when(mockContext.with(any(Span.class))).thenReturn(mockContext);
        // Given: An item with only 1 available unit
        String itemId = "limited-item";
        LocalDateTime futureDate = LocalDateTime.now().plusDays(7);
        
        InventoryItem item = new InventoryItem();
        item.setId(itemId);
        item.setName("Limited Item");
        item.setAvailableQuantity(1); // Only 1 available
        
        // First reservation request - should succeed
        UUID firstCorrelationId = UUID.randomUUID();
        UUID firstEventId = UUID.randomUUID();
        Map<String, Integer> firstItemQuantities = new HashMap<>();
        firstItemQuantities.put(itemId, 1);
        ReservationRequestedEvent firstEvent = new ReservationRequestedEvent(
                firstCorrelationId, "user1", firstItemQuantities, futureDate, LocalDateTime.now());
        
        String firstEventPayload = "{\"correlationId\":\"" + firstCorrelationId + "\"}";
        when(eventProcessingService.isEventProcessed(firstEventId)).thenReturn(false);
        when(objectMapper.writeValueAsString(firstEvent)).thenReturn(firstEventPayload);
        when(inventoryItemMapper.findById(itemId)).thenReturn(item);
        when(reservationMapper.sumConfirmedReservationQuantitiesForItemOnDate(itemId, futureDate))
                .thenReturn(0L) // No existing reservations initially
                .thenReturn(1L); // After first reservation, 1 is reserved
        doNothing().when(reservationMapper).insert(any(Reservation.class));
        doNothing().when(reservationMapper).insertReservationItem(anyString(), anyString(), anyInt());
        when(objectMapper.writeValueAsString(any(ReservationCreatedEvent.class)))
                .thenReturn("{\"confirmationNumber\":\"CONF-001\"}");

        @SuppressWarnings("unchecked")
        SendResult<String, String> mockSendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mockSendResult);
        lenient().when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        // When: Process first reservation (should succeed)
        persistenceService.processReservationRequest(firstEvent, firstEventId);

        // Then: First reservation should succeed
        verify(reservationMapper).insert(any(Reservation.class));
        verify(kafkaTemplate).send(eq("reservation-created"), eq(firstCorrelationId.toString()), anyString());
        
        // Now: Second reservation request for same item on same date - should fail
        UUID secondCorrelationId = UUID.randomUUID();
        UUID secondEventId = UUID.randomUUID();
        Map<String, Integer> secondItemQuantities = new HashMap<>();
        secondItemQuantities.put(itemId, 1);
        ReservationRequestedEvent secondEvent = new ReservationRequestedEvent(
                secondCorrelationId, "user2", secondItemQuantities, futureDate, LocalDateTime.now());
        
        String secondEventPayload = "{\"correlationId\":\"" + secondCorrelationId + "\"}";
        when(eventProcessingService.isEventProcessed(secondEventId)).thenReturn(false);
        when(objectMapper.writeValueAsString(secondEvent)).thenReturn(secondEventPayload);
        when(objectMapper.writeValueAsString(any(ReservationFailedEvent.class)))
                .thenReturn("{\"reason\":\"Insufficient availability\"}");

        // When: Process second reservation (should fail)
        persistenceService.processReservationRequest(secondEvent, secondEventId);

        // Then: Second reservation should fail
        verify(reservationMapper, times(1)).insert(any(Reservation.class)); // Only first one saved
        verify(kafkaTemplate).send(eq("reservation-failed"), eq(secondCorrelationId.toString()), anyString());
        
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        
        // Verify the failed event was published with correct reason
        verify(kafkaTemplate, atLeastOnce()).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());
        
        // Find the reservation-failed event
        List<String> topics = topicCaptor.getAllValues();
        List<String> values = valueCaptor.getAllValues();
        boolean foundFailedEvent = false;
        for (int i = 0; i < topics.size(); i++) {
            if ("reservation-failed".equals(topics.get(i))) {
                assertTrue(values.get(i).contains("Insufficient availability"));
                foundFailedEvent = true;
                break;
            }
        }
        assertTrue(foundFailedEvent, "Reservation failed event should be published");
        }
    }
}
