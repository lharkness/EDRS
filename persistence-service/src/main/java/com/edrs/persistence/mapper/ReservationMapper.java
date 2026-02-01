package com.edrs.persistence.mapper;

import com.edrs.persistence.entity.Reservation;
import com.edrs.persistence.entity.ReservationItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReservationMapper {
    
    void insert(Reservation reservation);
    
    Reservation findByConfirmationNumber(String confirmationNumber);
    
    void update(Reservation reservation);
    
    /**
     * Counts confirmed reservations for a specific inventory item on a specific date.
     * Used for availability checking.
     */
    long countConfirmedReservationsForItemOnDate(
            @Param("itemId") String itemId,
            @Param("reservationDate") LocalDateTime reservationDate);
    
    /**
     * Finds all confirmed reservations for a specific inventory item on a specific date.
     */
    List<Reservation> findConfirmedReservationsForItemOnDate(
            @Param("itemId") String itemId,
            @Param("reservationDate") LocalDateTime reservationDate);
    
    /**
     * Counts confirmed reservations for a specific inventory item in a date range.
     * Used for calculating effective availability.
     */
    long countConfirmedReservationsForItemInDateRange(
            @Param("itemId") String itemId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    /**
     * Sums quantities from confirmed reservations for a specific inventory item on a specific date.
     * Used for availability checking with quantities.
     */
    long sumConfirmedReservationQuantitiesForItemOnDate(
            @Param("itemId") String itemId,
            @Param("reservationDate") LocalDateTime reservationDate);
    
    /**
     * Sums quantities from confirmed reservations for a specific inventory item in a date range.
     * Used for calculating effective availability with quantities.
     */
    long sumConfirmedReservationQuantitiesForItemInDateRange(
            @Param("itemId") String itemId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    void insertReservationItem(@Param("confirmationNumber") String confirmationNumber, 
                               @Param("inventoryItemId") String inventoryItemId,
                               @Param("quantity") Integer quantity);
    
    /**
     * Finds reservation items with quantities for a confirmation number.
     */
    List<ReservationItem> findReservationItems(String confirmationNumber);
    
    /**
     * Legacy method for backward compatibility - returns just item IDs.
     */
    @Deprecated
    List<String> findReservationItemIds(String confirmationNumber);
    
    void deleteReservationItems(String confirmationNumber);
}
