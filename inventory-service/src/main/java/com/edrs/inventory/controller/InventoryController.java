package com.edrs.inventory.controller;

import com.edrs.inventory.dto.InventoryFilter;
import com.edrs.inventory.dto.InventoryItem;
import com.edrs.inventory.dto.InventoryReceiveRequest;
import com.edrs.inventory.service.CsvInventoryParser;
import com.edrs.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory API", description = "API for managing inventory")
public class InventoryController {
    private final InventoryService inventoryService;
    private final CsvInventoryParser csvParser;

    public InventoryController(InventoryService inventoryService, CsvInventoryParser csvParser) {
        this.inventoryService = inventoryService;
        this.csvParser = csvParser;
    }

    @GetMapping
    @Operation(summary = "List inventory items", description = "Returns all inventory items matching the filter criteria")
    public ResponseEntity<List<InventoryItem>> listInventory(@ModelAttribute InventoryFilter filter) {
        List<InventoryItem> items = inventoryService.listInventory(filter);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Show inventory item details", description = "Retrieves details for a specific inventory item")
    public ResponseEntity<InventoryItem> showInventoryItem(
            @Parameter(name = "id", description = "Inventory item ID", required = true, example = "item1", in = ParameterIn.PATH)
            @PathVariable("id") String id) {
        InventoryItem item = inventoryService.getInventoryItem(id);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    @PostMapping
    @Operation(summary = "Add inventory item", description = "Creates a new inventory item with the specified details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Inventory item created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid inventory item data", content = @Content),
        @ApiResponse(responseCode = "409", description = "Inventory item with this ID already exists", content = @Content)
    })
    public ResponseEntity<InventoryItem> addInventoryItem(@Valid @RequestBody InventoryItem item) {
        // Check if item already exists
        InventoryItem existing = inventoryService.getInventoryItem(item.getId());
        if (existing != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        
        inventoryService.updateInventoryItem(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @GetMapping("/{id}/availability")
    @Operation(summary = "Get effective available quantity", description = "Returns the effective available quantity for an item on a given date, accounting for all confirmed reservations from now until that date")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Effective available quantity calculated successfully"),
        @ApiResponse(responseCode = "404", description = "Inventory item not found", content = @Content)
    })
    public ResponseEntity<AvailabilityResponse> getEffectiveAvailability(
            @Parameter(name = "id", description = "Inventory item ID", required = true, example = "item1", in = ParameterIn.PATH)
            @PathVariable("id") String id,
            @Parameter(name = "date", description = "Target date to check availability for (ISO 8601 format)", required = true, example = "2026-02-15T10:00:00Z", in = ParameterIn.QUERY)
            @RequestParam("date") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime date) {
        Integer effectiveQuantity = inventoryService.getEffectiveAvailableQuantity(id, date);
        if (effectiveQuantity == null) {
            return ResponseEntity.notFound().build();
        }
        
        InventoryItem item = inventoryService.getInventoryItem(id);
        AvailabilityResponse response = new AvailabilityResponse();
        response.setItemId(id);
        response.setItemName(item != null ? item.getName() : null);
        response.setBaseAvailableQuantity(item != null ? item.getAvailableQuantity() : null);
        response.setEffectiveAvailableQuantity(effectiveQuantity);
        response.setTargetDate(date);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/receive")
    @Operation(summary = "Receive inventory", description = "Processes inventory receive records")
    public ResponseEntity<Void> receiveInventory(@Valid @RequestBody InventoryReceiveRequest request) {
        inventoryService.receiveInventory(request.getReceiveRecords());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping(value = "/receive/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Bulk receive inventory from CSV",
        description = "Uploads a CSV file to bulk import inventory. CSV format: inventoryItemId,quantity. First line can be a header."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "CSV file accepted and processing started"),
        @ApiResponse(responseCode = "400", description = "Invalid CSV file format", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error processing CSV file", content = @Content)
    })
    public ResponseEntity<BulkImportResponse> bulkReceiveInventory(
            @RequestParam("file") MultipartFile file) {
        try {
            List<InventoryReceiveRequest.ReceiveRecord> records = csvParser.parseCsv(file);
            
            if (records.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new BulkImportResponse(0, 0, "No valid records found in CSV file"));
            }
            
            inventoryService.receiveInventory(records);
            
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new BulkImportResponse(records.size(), 0, "CSV file processed successfully"));
                
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new BulkImportResponse(0, 0, "Error parsing CSV: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BulkImportResponse(0, 0, "Error processing CSV: " + e.getMessage()));
        }
    }

    /**
     * Response DTO for effective availability.
     */
    @Schema(description = "Response for effective available quantity calculation")
    public static class AvailabilityResponse {
        @Schema(description = "Inventory item ID", example = "item1")
        private String itemId;
        
        @Schema(description = "Inventory item name", example = "Laptop")
        private String itemName;
        
        @Schema(description = "Base available quantity (before reservations)", example = "10")
        private Integer baseAvailableQuantity;
        
        @Schema(description = "Effective available quantity (after accounting for reservations)", example = "7")
        private Integer effectiveAvailableQuantity;
        
        @Schema(description = "Target date for availability check", example = "2026-02-15T10:00:00Z")
        private java.time.LocalDateTime targetDate;

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public Integer getBaseAvailableQuantity() {
            return baseAvailableQuantity;
        }

        public void setBaseAvailableQuantity(Integer baseAvailableQuantity) {
            this.baseAvailableQuantity = baseAvailableQuantity;
        }

        public Integer getEffectiveAvailableQuantity() {
            return effectiveAvailableQuantity;
        }

        public void setEffectiveAvailableQuantity(Integer effectiveAvailableQuantity) {
            this.effectiveAvailableQuantity = effectiveAvailableQuantity;
        }

        public java.time.LocalDateTime getTargetDate() {
            return targetDate;
        }

        public void setTargetDate(java.time.LocalDateTime targetDate) {
            this.targetDate = targetDate;
        }
    }

    /**
     * Response DTO for bulk import operations.
     */
    @Schema(description = "Response for bulk inventory import")
    public static class BulkImportResponse {
        @Schema(description = "Number of records successfully processed", example = "10")
        private final int recordsProcessed;
        
        @Schema(description = "Number of records that failed", example = "0")
        private final int recordsFailed;
        
        @Schema(description = "Status message", example = "CSV file processed successfully")
        private final String message;

        public BulkImportResponse(int recordsProcessed, int recordsFailed, String message) {
            this.recordsProcessed = recordsProcessed;
            this.recordsFailed = recordsFailed;
            this.message = message;
        }

        public int getRecordsProcessed() {
            return recordsProcessed;
        }

        public int getRecordsFailed() {
            return recordsFailed;
        }

        public String getMessage() {
            return message;
        }
    }
}
