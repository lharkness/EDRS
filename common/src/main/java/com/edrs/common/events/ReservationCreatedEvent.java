package com.edrs.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ReservationCreatedEvent {
    private final UUID correlationId;
    private final String confirmationNumber;
    private final String userId;
    private final List<String> inventoryItemIds;
    private final LocalDateTime reservationDate;
    private final LocalDateTime timestamp;

    @JsonCreator
    public ReservationCreatedEvent(
            @JsonProperty("correlationId") UUID correlationId,
            @JsonProperty("confirmationNumber") String confirmationNumber,
            @JsonProperty("userId") String userId,
            @JsonProperty("inventoryItemIds") List<String> inventoryItemIds,
            @JsonProperty("reservationDate") LocalDateTime reservationDate,
            @JsonProperty("timestamp") LocalDateTime timestamp) {
        this.correlationId = correlationId;
        this.confirmationNumber = confirmationNumber;
        this.userId = userId;
        this.inventoryItemIds = inventoryItemIds;
        this.reservationDate = reservationDate;
        this.timestamp = timestamp;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
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
