package com.edrs.inventory.controller;

import com.edrs.inventory.dto.InventoryFilter;
import com.edrs.inventory.dto.InventoryItem;
import com.edrs.inventory.dto.InventoryReceiveRequest;
import com.edrs.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory API", description = "API for managing inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
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
}
