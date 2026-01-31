package com.edrs.reservation.controller;

import com.edrs.reservation.dto.MakeReservationRequest;
import com.edrs.reservation.dto.ReservationResponse;
import com.edrs.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservation API", description = "API for managing reservations")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Make a reservation", description = "Creates a new reservation request")
    public ResponseEntity<ReservationResponse> makeReservation(@Valid @RequestBody MakeReservationRequest request) {
        String correlationId = reservationService.makeReservation(
                request.getUserId(),
                request.getInventoryItemIds(),
                request.getReservationDate()
        );

        ReservationResponse response = new ReservationResponse();
        response.setConfirmationNumber(correlationId);
        response.setUserId(request.getUserId());
        response.setInventoryItemIds(request.getInventoryItemIds());
        response.setReservationDate(request.getReservationDate());
        response.setStatus("PENDING");

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/{confirmationNumber}/cancel")
    @Operation(summary = "Cancel a reservation", description = "Cancels an existing reservation")
    public ResponseEntity<Void> cancelReservation(@PathVariable String confirmationNumber) {
        reservationService.cancelReservation(confirmationNumber);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/{confirmationNumber}")
    @Operation(summary = "Show reservation details", description = "Retrieves reservation details by confirmation number")
    public ResponseEntity<ReservationResponse> showReservation(@PathVariable String confirmationNumber) {
        ReservationResponse reservation = reservationService.getReservation(confirmationNumber);
        if (reservation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reservation);
    }
}
