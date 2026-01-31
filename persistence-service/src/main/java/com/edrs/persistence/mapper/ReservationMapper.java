package com.edrs.persistence.mapper;

import com.edrs.persistence.entity.Reservation;
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
    
    void insertReservationItem(@Param("confirmationNumber") String confirmationNumber, 
                               @Param("inventoryItemId") String inventoryItemId);
    
    List<String> findReservationItemIds(String confirmationNumber);
    
    void deleteReservationItems(String confirmationNumber);
}
