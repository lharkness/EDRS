package com.edrs.reservation.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ReservationResponse {
    private String confirmationNumber;
    private String userId;
    private List<String> inventoryItemIds;
    private LocalDateTime reservationDate;
    private String status;

    public ReservationResponse() {
    }

    public ReservationResponse(String confirmationNumber, String userId, List<String> inventoryItemIds, 
                              LocalDateTime reservationDate, String status) {
        this.confirmationNumber = confirmationNumber;
        this.userId = userId;
        this.inventoryItemIds = inventoryItemIds;
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

    public List<String> getInventoryItemIds() {
        return inventoryItemIds;
    }

    public void setInventoryItemIds(List<String> inventoryItemIds) {
        this.inventoryItemIds = inventoryItemIds;
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
