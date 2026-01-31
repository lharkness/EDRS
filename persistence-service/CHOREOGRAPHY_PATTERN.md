# Choreography Pattern Implementation

## Overview

The persistence service has been redesigned to follow **choreography-style event-driven architecture** best practices for Kafka.

## Key Features Implemented

### 1. Idempotent Event Processing
- **ProcessedEvent** table tracks all processed events by `eventId`
- Prevents duplicate processing of the same event
- Deterministic event ID generation from Kafka record (partition + offset + correlationId)

### 2. Event Sourcing
- **EventLog** table stores all events for audit and replay capabilities
- Tracks event metadata: eventId, correlationId, eventType, version, source, payload
- Enables event replay and debugging

### 3. Database Schema Improvements
- Proper indexes on frequently queried columns
- Foreign key constraints and data integrity
- Optimized for read and write operations

### 4. Transactional Event Publishing
- Events are published after successful database commit
- Ready for outbox pattern implementation (transactional outbox table can be added)
- Proper error handling and logging

### 5. Choreography Pattern Principles
- **Decoupled Services**: Services communicate only through events
- **Event-Driven**: Each service reacts to events and publishes new events
- **No Central Orchestrator**: Each service is autonomous
- **Event Flow**:
  - Reservation Service → publishes `reservation-requested`
  - Persistence Service → listens, persists, publishes `reservation-created`
  - Notification Service → listens to `reservation-created`, sends email
  - Logging Service → listens to all events, logs with correlation IDs

## Database Schema

### Tables

#### `reservations`
- Primary key: `confirmation_number`
- Indexes: `user_id`, `status`, `reservation_date`, `created_at`
- Tracks reservation state

#### `reservation_items`
- Join table for reservation inventory items
- Foreign key to `reservations.confirmation_number`

#### `inventory_items`
- Primary key: `id`
- Indexes: `category`, `available_quantity`
- Tracks inventory state

#### `event_log` (NEW)
- Event sourcing table
- Stores all events with full payload
- Indexes: `event_id` (unique), `correlation_id`, `event_type`, `timestamp`
- Enables event replay and audit

#### `processed_events` (NEW)
- Idempotency tracking
- Prevents duplicate event processing
- Indexes: `event_id` (unique), `correlation_id`

## Event Flow Example

```
1. User creates reservation via REST API
   ↓
2. Reservation Service publishes: reservation-requested
   ↓
3. Persistence Service:
   - Checks idempotency (processed_events)
   - Logs event (event_log)
   - Persists reservation (reservations table)
   - Marks as processed
   - Publishes: reservation-created
   ↓
4. Notification Service listens to reservation-created → sends email
5. Logging Service listens to all events → logs with correlation ID
```

## Best Practices Implemented

1. **Idempotency**: Events can be safely reprocessed
2. **Event Sourcing**: Full audit trail of all events
3. **Correlation IDs**: Track requests across services
4. **Error Handling**: Proper logging and error propagation
5. **Transactional Integrity**: Database and event publishing coordination
6. **Schema Design**: Proper indexes and constraints

## Future Enhancements

1. **Transactional Outbox Pattern**: Add outbox table for guaranteed event delivery
2. **Dead Letter Queue**: Handle failed events
3. **Event Versioning**: Support multiple event versions
4. **Saga Pattern**: For distributed transactions if needed
5. **Event Replay**: Replay events from event_log for recovery

## Configuration

- Kafka consumer: Manual acknowledgment enabled
- Isolation level: `read_committed` for transactional reads
- Producer: `acks=all` for guaranteed delivery
- Retries: 3 attempts for resilience
