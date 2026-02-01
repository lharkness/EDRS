-- Migration script to add quantity column to reservation_items table
-- This script should be run on existing databases

ALTER TABLE reservation_items 
ADD COLUMN IF NOT EXISTS quantity INTEGER NOT NULL DEFAULT 1;

-- Update existing rows to have quantity = 1 (assuming each reservation was for 1 unit)
UPDATE reservation_items 
SET quantity = 1 
WHERE quantity IS NULL OR quantity = 0;
