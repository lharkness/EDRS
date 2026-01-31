package com.edrs.common.util;

import java.util.UUID;

public class CorrelationIdUtil {
    private static final ThreadLocal<UUID> correlationIdHolder = new ThreadLocal<>();

    public static UUID generateCorrelationId() {
        UUID correlationId = UUID.randomUUID();
        correlationIdHolder.set(correlationId);
        return correlationId;
    }

    public static UUID getCorrelationId() {
        return correlationIdHolder.get();
    }

    public static void setCorrelationId(UUID correlationId) {
        correlationIdHolder.set(correlationId);
    }

    public static void clearCorrelationId() {
        correlationIdHolder.remove();
    }
}
