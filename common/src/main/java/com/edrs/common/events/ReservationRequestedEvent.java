package com.edrs.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class ReservationRequestedEvent {
    private final UUID correlationId;
    private final String userId;
    private final Map<String, Integer> inventoryItemQuantities;
    private final LocalDateTime reservationDate;
    private final LocalDateTime timestamp;

    @JsonCreator
    public ReservationRequestedEvent(
            @JsonProperty("correlationId") UUID correlationId,
            @JsonProperty("userId") String userId,
            @JsonProperty("inventoryItemQuantities") Map<String, Integer> inventoryItemQuantities,
            @JsonProperty("reservationDate") LocalDateTime reservationDate,
            @JsonProperty("timestamp") LocalDateTime timestamp) {
        this.correlationId = correlationId;
        this.userId = userId;
        this.inventoryItemQuantities = inventoryItemQuantities;
        this.reservationDate = reservationDate;
        this.timestamp = timestamp;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, Integer> getInventoryItemQuantities() {
        return inventoryItemQuantities;
    }

    public LocalDateTime getReservationDate() {
        return reservationDate;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
