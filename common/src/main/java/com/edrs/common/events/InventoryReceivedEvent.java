package com.edrs.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class InventoryReceivedEvent {
    private final UUID correlationId;
    private final List<InventoryReceiveRecord> receiveRecords;
    private final LocalDateTime timestamp;

    @JsonCreator
    public InventoryReceivedEvent(
            @JsonProperty("correlationId") UUID correlationId,
            @JsonProperty("receiveRecords") List<InventoryReceiveRecord> receiveRecords,
            @JsonProperty("timestamp") LocalDateTime timestamp) {
        this.correlationId = correlationId;
        this.receiveRecords = receiveRecords;
        this.timestamp = timestamp;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public List<InventoryReceiveRecord> getReceiveRecords() {
        return receiveRecords;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public static class InventoryReceiveRecord {
        private final String inventoryItemId;
        private final int quantity;

        @JsonCreator
        public InventoryReceiveRecord(
                @JsonProperty("inventoryItemId") String inventoryItemId,
                @JsonProperty("quantity") int quantity) {
            this.inventoryItemId = inventoryItemId;
            this.quantity = quantity;
        }

        public String getInventoryItemId() {
            return inventoryItemId;
        }

        public int getQuantity() {
            return quantity;
        }
    }
}
