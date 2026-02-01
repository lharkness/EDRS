package com.edrs.reservation.controller;

import com.edrs.reservation.dto.MakeReservationRequest;
import com.edrs.reservation.dto.ReservationResponse;
import com.edrs.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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

    @GetMapping
    @Operation(summary = "List all reservations", description = "Returns all reservations, optionally filtered by userId")
    public ResponseEntity<java.util.List<ReservationResponse>> listReservations(
            @Parameter(description = "Optional user ID to filter reservations", required = false, example = "user1")
            @RequestParam(value = "userId", required = false) String userId) {
        java.util.List<ReservationResponse> reservations = reservationService.listReservations(userId);
        return ResponseEntity.ok(reservations);
    }

    @PostMapping
    @Operation(summary = "Make a reservation", description = "Creates a new reservation request with quantities for each inventory item")
    public ResponseEntity<ReservationResponse> makeReservation(@Valid @RequestBody MakeReservationRequest request) {
        String correlationId = reservationService.makeReservation(
                request.getUserId(),
                request.getInventoryItemQuantities(),
                request.getReservationDate()
        );

        ReservationResponse response = new ReservationResponse();
        response.setConfirmationNumber(correlationId);
        response.setUserId(request.getUserId());
        response.setInventoryItemQuantities(request.getInventoryItemQuantities());
        response.setReservationDate(request.getReservationDate());
        response.setStatus("PENDING");

        // Store immediately so it can be queried even while pending
        reservationService.updateReservation(response);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/{confirmationNumber}/cancel")
    @Operation(summary = "Cancel a reservation", description = "Cancels an existing reservation")
    public ResponseEntity<Void> cancelReservation(
            @Parameter(name = "confirmationNumber", description = "Confirmation number of the reservation to cancel", required = true, example = "fb4279de-a446-4a2c-928c-50e997c2d450", in = ParameterIn.PATH)
            @PathVariable("confirmationNumber") String confirmationNumber) {
        reservationService.cancelReservation(confirmationNumber);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/{confirmationNumber}")
    @Operation(summary = "Show reservation details", description = "Retrieves reservation details by confirmation number")
    public ResponseEntity<ReservationResponse> showReservation(
            @Parameter(name = "confirmationNumber", description = "Confirmation number of the reservation", required = true, example = "fb4279de-a446-4a2c-928c-50e997c2d450", in = ParameterIn.PATH)
            @PathVariable("confirmationNumber") String confirmationNumber) {
        ReservationResponse reservation = reservationService.getReservation(confirmationNumber);
        if (reservation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reservation);
    }
}
