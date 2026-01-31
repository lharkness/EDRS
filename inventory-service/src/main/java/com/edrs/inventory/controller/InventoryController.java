package com.edrs.inventory.controller;

import com.edrs.inventory.dto.InventoryFilter;
import com.edrs.inventory.dto.InventoryItem;
import com.edrs.inventory.dto.InventoryReceiveRequest;
import com.edrs.inventory.service.CsvInventoryParser;
import com.edrs.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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
    public ResponseEntity<InventoryItem> showInventoryItem(@PathVariable String id) {
        InventoryItem item = inventoryService.getInventoryItem(id);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
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
