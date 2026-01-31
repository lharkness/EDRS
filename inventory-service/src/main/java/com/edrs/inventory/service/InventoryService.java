package com.edrs.inventory.service;

import com.edrs.common.events.InventoryReceivedEvent;
import com.edrs.common.util.CorrelationIdUtil;
import com.edrs.inventory.dto.InventoryFilter;
import com.edrs.inventory.dto.InventoryItem;
import com.edrs.inventory.dto.InventoryReceiveRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

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

    @Autowired
    public InventoryService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        initializeSampleInventory();
    }

    private void initializeSampleInventory() {
        inventoryStore.put("item1", new InventoryItem("item1", "Laptop", "Dell Laptop", 10, "Electronics"));
        inventoryStore.put("item2", new InventoryItem("item2", "Projector", "HD Projector", 5, "Electronics"));
        inventoryStore.put("item3", new InventoryItem("item3", "Conference Room A", "Large conference room", 1, "Rooms"));
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
                item.setAvailableQuantity(item.getAvailableQuantity() + record.getQuantity());
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
                .map(record -> new InventoryReceivedEvent.InventoryReceiveRecord(
                        record.getInventoryItemId(),
                        record.getQuantity()
                ))
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
    }
}
