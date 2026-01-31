package com.edrs.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ReservationRequestedEvent {
    private final UUID correlationId;
    private final String userId;
    private final List<String> inventoryItemIds;
    private final LocalDateTime reservationDate;
    private final LocalDateTime timestamp;

    @JsonCreator
    public ReservationRequestedEvent(
            @JsonProperty("correlationId") UUID correlationId,
            @JsonProperty("userId") String userId,
            @JsonProperty("inventoryItemIds") List<String> inventoryItemIds,
            @JsonProperty("reservationDate") LocalDateTime reservationDate,
            @JsonProperty("timestamp") LocalDateTime timestamp) {
        this.correlationId = correlationId;
        this.userId = userId;
        this.inventoryItemIds = inventoryItemIds;
        this.reservationDate = reservationDate;
        this.timestamp = timestamp;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public String getUserId() {
        return userId;
    }

    public List<String> getInventoryItemIds() {
        return inventoryItemIds;
    }

    public LocalDateTime getReservationDate() {
        return reservationDate;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
