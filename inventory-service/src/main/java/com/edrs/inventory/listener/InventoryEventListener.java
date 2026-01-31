package com.edrs.inventory.listener;

import com.edrs.common.events.InventoryReceivedEvent;
import com.edrs.common.util.CorrelationIdUtil;
import com.edrs.inventory.dto.InventoryItem;
import com.edrs.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventListener {
    private static final Logger logger = LoggerFactory.getLogger(InventoryEventListener.class);
    private static final String INVENTORY_RECEIVED_TOPIC = "inventory-received";

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    public InventoryEventListener(InventoryService inventoryService, ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
    }

    // Note: This listener is kept for potential future use if persistence service publishes
    // confirmation events. Currently, inventory is updated optimistically in the service.
    // Uncomment if you want to sync from persistence service events.
    /*
    @KafkaListener(topics = INVENTORY_RECEIVED_TOPIC, groupId = "inventory-service-group")
    public void handleInventoryReceived(String message) {
        try {
            CorrelationIdUtil.setCorrelationId(null); // Will be set from event
            InventoryReceivedEvent event = objectMapper.readValue(message, InventoryReceivedEvent.class);
            CorrelationIdUtil.setCorrelationId(event.getCorrelationId());
            
            logger.info("Received inventory received event with correlationId: {}", event.getCorrelationId());
            // Inventory is already updated optimistically in the service
        } catch (Exception e) {
            logger.error("Error processing inventory received event", e);
        }
    }
    */
}
