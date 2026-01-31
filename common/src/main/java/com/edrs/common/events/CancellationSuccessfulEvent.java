package com.edrs.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

public class CancellationSuccessfulEvent {
    private final UUID correlationId;
    private final String confirmationNumber;
    private final String userId;
    private final LocalDateTime timestamp;

    @JsonCreator
    public CancellationSuccessfulEvent(
            @JsonProperty("correlationId") UUID correlationId,
            @JsonProperty("confirmationNumber") String confirmationNumber,
            @JsonProperty("userId") String userId,
            @JsonProperty("timestamp") LocalDateTime timestamp) {
        this.correlationId = correlationId;
        this.confirmationNumber = confirmationNumber;
        this.userId = userId;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
