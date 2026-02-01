package com.edrs.inventory.service;

import com.edrs.common.events.InventoryReceivedEvent;
import com.edrs.common.util.CorrelationIdUtil;
import com.edrs.inventory.dto.InventoryFilter;
import com.edrs.inventory.dto.InventoryItem;
import com.edrs.inventory.dto.InventoryReceiveRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryService {
    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);
    private static final String INVENTORY_RECEIVED_TOPIC = "inventory-received";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, InventoryItem> inventoryStore = new HashMap<>();
    private final RestTemplate restTemplate;
    
    @Value("${persistence.service.url:http://localhost:8084}")
    private String persistenceServiceUrl;

    @Autowired
    public InventoryService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        initializeSampleInventory();
    }

    private void initializeSampleInventory() {
        inventoryStore.put("item1", new InventoryItem("item1", "Laptop", "Dell Laptop", Integer.valueOf(10), "Electronics"));
        inventoryStore.put("item2", new InventoryItem("item2", "Projector", "HD Projector", Integer.valueOf(5), "Electronics"));
        inventoryStore.put("item3", new InventoryItem("item3", "Conference Room A", "Large conference room", Integer.valueOf(1), "Rooms"));
    }

    public List<InventoryItem> listInventory(InventoryFilter filter) {
        logger.info("Listing inventory with filter: {}", filter);
        return inventoryStore.values().stream()
                .filter(item -> matchesFilter(item, filter))
                .collect(Collectors.toList());
    }

    public InventoryItem getInventoryItem(String id) {
        logger.info("Getting inventory item: {}", id);
        return inventoryStore.get(id);
    }

    public void receiveInventory(List<InventoryReceiveRequest.ReceiveRecord> receiveRecords) {
        UUID correlationId = CorrelationIdUtil.generateCorrelationId();
        logger.info("Receiving inventory with correlationId: {}", correlationId);

        // Update local cache optimistically
        for (InventoryReceiveRequest.ReceiveRecord record : receiveRecords) {
            InventoryItem item = inventoryStore.get(record.getInventoryItemId());
if (item != null) {
                Integer currentQuantity = item.getAvailableQuantity() != null ? item.getAvailableQuantity() : 0;
                item.setAvailableQuantity(currentQuantity + record.getQuantity());
                inventoryStore.put(record.getInventoryItemId(), item);
                logger.info("Updated local cache for item {} with quantity {}", record.getInventoryItemId(), item.getAvailableQuantity());
            } else {
                // Create new item if it doesn't exist
                InventoryItem newItem = new InventoryItem();
                newItem.setId(record.getInventoryItemId());
                newItem.setName("Item " + record.getInventoryItemId());
                newItem.setDescription("Auto-created item");
                newItem.setCategory("General");
                newItem.setAvailableQuantity(record.getQuantity());
                inventoryStore.put(record.getInventoryItemId(), newItem);
                logger.info("Created new item in local cache: {}", record.getInventoryItemId());
            }
        }

        List<InventoryReceivedEvent.InventoryReceiveRecord> eventRecords = receiveRecords.stream()
                .map(record -> {
                    // Get the full item details from the local cache (updated above)
                    InventoryItem item = inventoryStore.get(record.getInventoryItemId());
                    return new InventoryReceivedEvent.InventoryReceiveRecord(
                            record.getInventoryItemId(),
                            record.getQuantity(),
                            item != null ? item.getName() : null,
                            item != null ? item.getDescription() : null,
                            item != null ? item.getCategory() : null
                    );
                })
                .collect(Collectors.toList());

        InventoryReceivedEvent event = new InventoryReceivedEvent(
                correlationId,
                eventRecords,
                LocalDateTime.now()
        );

        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(INVENTORY_RECEIVED_TOPIC, correlationId.toString(), eventJson);
            logger.info("Published inventory received event with correlationId: {}", correlationId);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing inventory received event", e);
            throw new RuntimeException("Failed to process inventory receive", e);
        }
    }

    private boolean matchesFilter(InventoryItem item, InventoryFilter filter) {
        if (filter == null) {
            return true;
        }

        if (filter.getCategory() != null && !filter.getCategory().equals(item.getCategory())) {
            return false;
        }

        if (filter.getMinQuantity() != null && item.getAvailableQuantity() < filter.getMinQuantity()) {
            return false;
        }

        if (filter.getNameContains() != null && !item.getName().toLowerCase().contains(filter.getNameContains().toLowerCase())) {
            return false;
        }

        return true;
    }

    public void updateInventoryItem(InventoryItem item) {
        inventoryStore.put(item.getId(), item);
        // Publish event to sync with persistence service
        publishInventoryReceivedEvent(item);
    }

    /**
     * Publishes an inventory-received event for a new or updated inventory item.
     * This ensures the persistence service stays in sync with inventory changes.
     */
    private void publishInventoryReceivedEvent(InventoryItem item) {
        UUID correlationId = CorrelationIdUtil.generateCorrelationId();
        logger.info("Publishing inventory received event for item {} with correlationId: {}", item.getId(), correlationId);

        // Create a receive record with the item's current available quantity and full metadata
        // If the item is new, use its availableQuantity; if updating, we treat it as receiving that quantity
        int quantity = item.getAvailableQuantity() != null ? item.getAvailableQuantity() : 0;
        
        InventoryReceivedEvent.InventoryReceiveRecord eventRecord = 
            new InventoryReceivedEvent.InventoryReceiveRecord(
                item.getId(), 
                quantity,
                item.getName(),
                item.getDescription(),
                item.getCategory()
            );
        
        InventoryReceivedEvent event = new InventoryReceivedEvent(
                correlationId,
                List.of(eventRecord),
                LocalDateTime.now()
        );

        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(INVENTORY_RECEIVED_TOPIC, correlationId.toString(), eventJson);
            logger.info("Published inventory received event for item {} with correlationId: {}", item.getId(), correlationId);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing inventory received event for item {}", item.getId(), e);
            // Don't throw - this is best-effort synchronization
        }
    }

    /**
     * Gets the effective available quantity for an inventory item on a given date.
     * This calculates: base availableQuantity - confirmed reservations from now until the requested date.
     * 
     * @param itemId The inventory item ID
     * @param targetDate The date to check availability for
     * @return The effective available quantity, or null if item not found
     */
    public Integer getEffectiveAvailableQuantity(String itemId, LocalDateTime targetDate) {
        InventoryItem item = inventoryStore.get(itemId);
        if (item == null) {
            logger.warn("Inventory item not found: {}", itemId);
            return null;
        }

        int baseQuantity = item.getAvailableQuantity() != null ? item.getAvailableQuantity() : 0;
        
        try {
            // Query persistence service for reservation quantities from now until target date
            LocalDateTime now = LocalDateTime.now();
            // Format dates in ISO format without nanoseconds for Spring's DateTimeFormat parser
            // ISO_DATE_TIME format: yyyy-MM-ddTHH:mm:ss (no nanoseconds)
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            String startDateStr = now.format(formatter);
            String endDateStr = targetDate.format(formatter);
            
            // Use UriComponentsBuilder for proper URL encoding
            org.springframework.web.util.UriComponentsBuilder uriBuilder = org.springframework.web.util.UriComponentsBuilder
                    .fromHttpUrl(persistenceServiceUrl + "/api/persistence/reservations/quantity")
                    .queryParam("itemId", itemId)
                    .queryParam("startDate", startDateStr)
                    .queryParam("endDate", endDateStr);
            
            java.net.URI uri = uriBuilder.build().toUri();
            logger.debug("Querying persistence service: {}", uri);
            Long reservedQuantity = restTemplate.getForObject(uri, Long.class);
            if (reservedQuantity == null) {
                reservedQuantity = 0L;
            }
            
            int effectiveQuantity = (int) (baseQuantity - reservedQuantity);
            logger.info("Effective available quantity for item {} on {}: {} (base: {}, reserved quantity: {})", 
                    itemId, targetDate, effectiveQuantity, baseQuantity, reservedQuantity);
            
            return Math.max(0, effectiveQuantity); // Don't return negative
        } catch (Exception e) {
            logger.error("Error querying persistence service for reservation quantities for item {}", itemId, e);
            // On error, return base quantity as fallback
            return baseQuantity;
        }
    }
}
