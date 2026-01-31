-- EDRS Database Schema
-- This file contains all CREATE TABLE statements for the EDRS persistence service

-- Business Tables

-- Inventory Items Table
CREATE TABLE IF NOT EXISTS inventory_items (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    available_quantity INTEGER NOT NULL DEFAULT 0,
    category VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);@@

CREATE INDEX IF NOT EXISTS idx_category ON inventory_items(category);@@
CREATE INDEX IF NOT EXISTS idx_available_quantity ON inventory_items(available_quantity);@@

-- Reservations Table
CREATE TABLE IF NOT EXISTS reservations (
    confirmation_number VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    reservation_date TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);@@

CREATE INDEX IF NOT EXISTS idx_user_id ON reservations(user_id);@@
CREATE INDEX IF NOT EXISTS idx_status ON reservations(status);@@
CREATE INDEX IF NOT EXISTS idx_reservation_date ON reservations(reservation_date);@@
CREATE INDEX IF NOT EXISTS idx_created_at ON reservations(created_at);@@

-- Reservation Items (Collection Table for Many-to-Many relationship)
CREATE TABLE IF NOT EXISTS reservation_items (
    confirmation_number VARCHAR(50) NOT NULL,
    inventory_item_id VARCHAR(100) NOT NULL,
    PRIMARY KEY (confirmation_number, inventory_item_id),
    FOREIGN KEY (confirmation_number) REFERENCES reservations(confirmation_number) ON DELETE CASCADE
);@@

-- Event Sourcing & Idempotency Tables

-- Event Log Table (Event Sourcing)
CREATE TABLE IF NOT EXISTS event_log (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    correlation_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_version VARCHAR(20),
    source VARCHAR(100),
    payload TEXT,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);@@

CREATE UNIQUE INDEX IF NOT EXISTS idx_event_id ON event_log(event_id);@@
CREATE INDEX IF NOT EXISTS idx_correlation_id ON event_log(correlation_id);@@
CREATE INDEX IF NOT EXISTS idx_event_type ON event_log(event_type);@@
CREATE INDEX IF NOT EXISTS idx_timestamp ON event_log(timestamp);@@

-- Processed Events Table (Idempotency)
CREATE TABLE IF NOT EXISTS processed_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    correlation_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handler_service VARCHAR(100)
);@@

CREATE UNIQUE INDEX IF NOT EXISTS idx_event_id_unique ON processed_events(event_id);@@
CREATE INDEX IF NOT EXISTS idx_correlation_id ON processed_events(correlation_id);@@

-- Triggers for updated_at timestamps

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;@@

-- Trigger for inventory_items
DROP TRIGGER IF EXISTS update_inventory_items_updated_at ON inventory_items;@@
CREATE TRIGGER update_inventory_items_updated_at
    BEFORE UPDATE ON inventory_items
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();@@

-- Trigger for reservations
DROP TRIGGER IF EXISTS update_reservations_updated_at ON reservations;@@
CREATE TRIGGER update_reservations_updated_at
    BEFORE UPDATE ON reservations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();@@
