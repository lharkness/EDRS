package com.edrs.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

public class CancellationRequestedEvent {
    private final UUID correlationId;
    private final String confirmationNumber;
    private final LocalDateTime timestamp;

    @JsonCreator
    public CancellationRequestedEvent(
            @JsonProperty("correlationId") UUID correlationId,
            @JsonProperty("confirmationNumber") String confirmationNumber,
            @JsonProperty("timestamp") LocalDateTime timestamp) {
        this.correlationId = correlationId;
        this.confirmationNumber = confirmationNumber;
        this.timestamp = timestamp;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
