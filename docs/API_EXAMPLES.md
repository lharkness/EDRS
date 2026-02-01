# API Usage Examples

This document provides practical examples of using the EDRS APIs.

## Reservation API Examples

### Create a Reservation with Quantities

Reservations now support quantities per item. Use a map/object where keys are item IDs and values are quantities.

**Request:**
```bash
curl -X 'POST' \
  'http://localhost:8080/api/reservations' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "userId": "user123",
  "inventoryItemQuantities": {
    "item1": 2,
    "item2": 5
  },
  "reservationDate": "2026-02-15T10:00:00Z"
}'
```

**Response:**
```json
{
  "confirmationNumber": "c997a097-c1fb-46ec-aca2-8566f25d2ae6",
  "userId": "user123",
  "inventoryItemQuantities": {
    "item1": 2,
    "item2": 5
  },
  "reservationDate": "2026-02-15T10:00:00Z",
  "status": "PENDING"
}
```

**Note:** The status will change to "CONFIRMED" once the persistence service processes the reservation.

### List All Reservations

```bash
curl -X 'GET' \
  'http://localhost:8080/api/reservations' \
  -H 'accept: */*'
```

### List Reservations by User

```bash
curl -X 'GET' \
  'http://localhost:8080/api/reservations?userId=user123' \
  -H 'accept: */*'
```

### Get Reservation Details

```bash
curl -X 'GET' \
  'http://localhost:8080/api/reservations/c997a097-c1fb-46ec-aca2-8566f25d2ae6' \
  -H 'accept: */*'
```

### Cancel a Reservation

```bash
curl -X 'POST' \
  'http://localhost:8080/api/reservations/c997a097-c1fb-46ec-aca2-8566f25d2ae6/cancel' \
  -H 'accept: */*'
```

## Inventory API Examples

### Add/Create Inventory Item

```bash
curl -X 'POST' \
  'http://localhost:8081/api/inventory' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "id": "item42",
  "name": "Notebook",
  "description": "Basic Notebook",
  "availableQuantity": 10,
  "category": "Supplies"
}'
```

### Get Inventory Item

```bash
curl -X 'GET' \
  'http://localhost:8081/api/inventory/item42' \
  -H 'accept: */*'
```

### Get Effective Available Quantity

This endpoint calculates the effective available quantity by subtracting confirmed reservations from the base quantity.

```bash
curl -X 'GET' \
  'http://localhost:8081/api/inventory/item42/availability?date=2026-02-15T10:00:00Z' \
  -H 'accept: */*'
```

**Response:**
```json
{
  "itemId": "item42",
  "itemName": "Notebook",
  "baseAvailableQuantity": 10,
  "effectiveAvailableQuantity": 8,
  "targetDate": "2026-02-15T10:00:00Z"
}
```

**Explanation:**
- `baseAvailableQuantity`: The base quantity of the item (10)
- `effectiveAvailableQuantity`: Base quantity minus confirmed reservations from now until the target date (10 - 2 = 8)
- This accounts for all confirmed reservations with their quantities

### List Inventory Items

```bash
# List all items
curl -X 'GET' \
  'http://localhost:8081/api/inventory' \
  -H 'accept: */*'

# Filter by category
curl -X 'GET' \
  'http://localhost:8081/api/inventory?category=Electronics' \
  -H 'accept: */*'

# Filter by minimum quantity
curl -X 'GET' \
  'http://localhost:8081/api/inventory?minQuantity=5' \
  -H 'accept: */*'
```

## Complete Workflow Example

### 1. Add Inventory Item

```bash
curl -X 'POST' \
  'http://localhost:8081/api/inventory' \
  -H 'Content-Type: application/json' \
  -d '{
  "id": "laptop-001",
  "name": "Dell Laptop",
  "description": "Dell XPS 15",
  "availableQuantity": 10,
  "category": "Electronics"
}'
```

### 2. Check Initial Availability

```bash
curl 'http://localhost:8081/api/inventory/laptop-001/availability?date=2026-02-20T10:00:00Z'
```

Response: `effectiveAvailableQuantity: 10` (no reservations yet)

### 3. Create Reservation for 3 Units

```bash
curl -X 'POST' \
  'http://localhost:8080/api/reservations' \
  -H 'Content-Type: application/json' \
  -d '{
  "userId": "alice",
  "inventoryItemQuantities": {
    "laptop-001": 3
  },
  "reservationDate": "2026-02-20T10:00:00Z"
}'
```

### 4. Check Availability Again

```bash
curl 'http://localhost:8081/api/inventory/laptop-001/availability?date=2026-02-20T10:00:00Z'
```

Response: `effectiveAvailableQuantity: 7` (10 - 3 = 7)

### 5. Create Another Reservation for 2 Units

```bash
curl -X 'POST' \
  'http://localhost:8080/api/reservations' \
  -H 'Content-Type: application/json' \
  -d '{
  "userId": "bob",
  "inventoryItemQuantities": {
    "laptop-001": 2
  },
  "reservationDate": "2026-02-20T10:00:00Z"
}'
```

### 6. Check Final Availability

```bash
curl 'http://localhost:8081/api/inventory/laptop-001/availability?date=2026-02-20T10:00:00Z'
```

Response: `effectiveAvailableQuantity: 5` (10 - 3 - 2 = 5)

## Important Notes

### Reservation Format Change

**Old format (deprecated):**
```json
{
  "userId": "user1",
  "inventoryItemIds": ["item1", "item2"],
  "reservationDate": "2026-02-15T10:00:00Z"
}
```

**New format (current):**
```json
{
  "userId": "user1",
  "inventoryItemQuantities": {
    "item1": 2,
    "item2": 1
  },
  "reservationDate": "2026-02-15T10:00:00Z"
}
```

### Date Format

All dates should be in ISO 8601 format:
- `2026-02-15T10:00:00Z` (UTC)
- `2026-02-15T10:00:00` (local time, no timezone)

### Availability Calculation

The effective availability endpoint:
- Queries all confirmed reservations from **now** until the **target date**
- Sums the **quantities** (not just counts) of reserved items
- Returns: `baseQuantity - sumOfReservedQuantities`

### Inventory Service Data Persistence

**Important:** The inventory service uses an **in-memory store**. Items added via the API are lost on service restart. Only items initialized in code (sample items) persist across restarts.

To persist inventory items:
1. Use the bulk CSV import feature
2. Or ensure items are added via API after each restart
3. Or implement persistent storage (future enhancement)
