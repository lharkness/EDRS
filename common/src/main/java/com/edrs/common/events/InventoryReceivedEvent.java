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
        private final String name;
        private final String description;
        private final String category;

        @JsonCreator
        public InventoryReceiveRecord(
                @JsonProperty("inventoryItemId") String inventoryItemId,
                @JsonProperty("quantity") int quantity,
                @JsonProperty("name") String name,
                @JsonProperty("description") String description,
                @JsonProperty("category") String category) {
            this.inventoryItemId = inventoryItemId;
            this.quantity = quantity;
            this.name = name;
            this.description = description;
            this.category = category;
        }

        public String getInventoryItemId() {
            return inventoryItemId;
        }

        public int getQuantity() {
            return quantity;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getCategory() {
            return category;
        }
    }
}
