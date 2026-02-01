package com.edrs.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Request to create a new reservation with quantities for each inventory item")
public class MakeReservationRequest {
    @NotEmpty(message = "User ID is required")
    @Schema(description = "ID of the user making the reservation", requiredMode = Schema.RequiredMode.REQUIRED, example = "user123")
    private String userId;

    @NotEmpty(message = "At least one inventory item is required")
    @Schema(description = "Map of inventory item IDs to quantities. Key is item ID, value is quantity to reserve.", 
            requiredMode = Schema.RequiredMode.REQUIRED, 
            example = "{\"item1\": 2, \"item2\": 1}",
            type = "object")
    private Map<String, @NotNull @Min(value = 1, message = "Quantity must be at least 1") Integer> inventoryItemQuantities;

    @NotNull(message = "Reservation date is required")
    @Schema(description = "Date and time for the reservation", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-02-15T10:00:00Z")
    private LocalDateTime reservationDate;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Map<String, Integer> getInventoryItemQuantities() {
        return inventoryItemQuantities;
    }

    public void setInventoryItemQuantities(Map<String, Integer> inventoryItemQuantities) {
        this.inventoryItemQuantities = inventoryItemQuantities;
    }

    public LocalDateTime getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(LocalDateTime reservationDate) {
        this.reservationDate = reservationDate;
    }
}
