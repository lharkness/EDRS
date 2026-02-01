package com.edrs.reservation.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class ReservationResponse {
    private String confirmationNumber;
    private String userId;
    private Map<String, Integer> inventoryItemQuantities;
    private LocalDateTime reservationDate;
    private String status;

    public ReservationResponse() {
    }

    public ReservationResponse(String confirmationNumber, String userId, Map<String, Integer> inventoryItemQuantities, 
                              LocalDateTime reservationDate, String status) {
        this.confirmationNumber = confirmationNumber;
        this.userId = userId;
        this.inventoryItemQuantities = inventoryItemQuantities;
        this.reservationDate = reservationDate;
        this.status = status;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public void setConfirmationNumber(String confirmationNumber) {
        this.confirmationNumber = confirmationNumber;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
