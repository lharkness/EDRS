package com.edrs.persistence.entity;

/**
 * Reservation Item entity representing the join table between reservations and inventory items.
 */
public class ReservationItem {
    private String confirmationNumber;
    private String inventoryItemId;
    private Integer quantity;

    public ReservationItem() {
    }

    public ReservationItem(String confirmationNumber, String inventoryItemId, Integer quantity) {
        this.confirmationNumber = confirmationNumber;
        this.inventoryItemId = inventoryItemId;
        this.quantity = quantity;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public void setConfirmationNumber(String confirmationNumber) {
        this.confirmationNumber = confirmationNumber;
    }

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
