package com.edrs.persistence.controller;

import com.edrs.persistence.service.PersistenceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/persistence")
public class PersistenceController {
    private final PersistenceService persistenceService;

    public PersistenceController(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @GetMapping("/reservations/count")
    @Deprecated
    public ResponseEntity<Long> countReservationsForItemInDateRange(
            @RequestParam("itemId") String itemId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        long count = persistenceService.countReservationsForItemInDateRange(itemId, startDate, endDate);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/reservations/quantity")
    public ResponseEntity<Long> sumReservationQuantitiesForItemInDateRange(
            @RequestParam("itemId") String itemId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        long quantity = persistenceService.sumReservationQuantitiesForItemInDateRange(itemId, startDate, endDate);
        return ResponseEntity.ok(quantity);
    }
}
