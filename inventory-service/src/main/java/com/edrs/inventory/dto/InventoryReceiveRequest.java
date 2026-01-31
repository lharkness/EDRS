package com.edrs.inventory.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class InventoryReceiveRequest {
    @NotEmpty(message = "At least one receive record is required")
    private List<ReceiveRecord> receiveRecords;

    public List<ReceiveRecord> getReceiveRecords() {
        return receiveRecords;
    }

    public void setReceiveRecords(List<ReceiveRecord> receiveRecords) {
        this.receiveRecords = receiveRecords;
    }

    public static class ReceiveRecord {
        @NotNull(message = "Inventory item ID is required")
        private String inventoryItemId;

        @NotNull(message = "Quantity is required")
        private Integer quantity;

        public String getInventoryItemId() {
            return inventoryItemId;
        }

        public void setInventoryItemId(String inventoryItemId) {
            this.inventoryItemId = inventoryItemId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
