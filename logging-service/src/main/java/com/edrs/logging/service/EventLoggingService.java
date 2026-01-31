package com.edrs.logging.service;

import com.edrs.common.util.CorrelationIdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EventLoggingService {
    private static final Logger logger = LoggerFactory.getLogger(EventLoggingService.class);
    private final ObjectMapper objectMapper;

    public EventLoggingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void logEvent(String topic, String eventType, String message, UUID correlationId) {
        if (correlationId != null) {
            CorrelationIdUtil.setCorrelationId(correlationId);
        }
        
        logger.info("[CORRELATION_ID: {}] [TOPIC: {}] [EVENT_TYPE: {}] Event: {}", 
                correlationId != null ? correlationId : "N/A", 
                topic, 
                eventType, 
                message);
    }

    public void logEvent(String topic, String eventType, Object event, UUID correlationId) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            logEvent(topic, eventType, eventJson, correlationId);
        } catch (Exception e) {
            logger.error("Error serializing event for logging", e);
            logEvent(topic, eventType, "Error serializing event", correlationId);
        }
    }
}
