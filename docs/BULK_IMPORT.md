# Bulk Inventory Import Guide

The Inventory Service supports bulk importing inventory via CSV file upload.

## CSV Format

The CSV file should have the following format:

```csv
inventoryItemId,quantity
item1,10
item2,5
item3,2
```

### Format Details

- **Header row (optional)**: The first line can be a header row. If it contains keywords like "inventoryItemId", "quantity", "item", or "id", it will be automatically skipped.
- **Columns**: 
  - Column 1: `inventoryItemId` (String, required)
  - Column 2: `quantity` (Integer, required, must be >= 0)
- **Delimiter**: Comma (`,`)
- **Quotes**: Fields can be quoted with double quotes (`"`) if they contain commas
- **Empty lines**: Automatically skipped
- **Encoding**: UTF-8

### Example CSV File

```csv
inventoryItemId,quantity
laptop-001,25
monitor-002,15
keyboard-003,50
mouse-004,30
```

## API Endpoint

### Upload CSV File

**POST** `/api/inventory/receive/bulk`

**Content-Type**: `multipart/form-data`

**Request Parameter**:
- `file`: CSV file (required)

**Response**:
```json
{
  "recordsProcessed": 10,
  "recordsFailed": 0,
  "message": "CSV file processed successfully"
}
```

## Usage Examples

### Using cURL

```bash
curl -X POST http://localhost:8081/api/inventory/receive/bulk \
  -F "file=@inventory.csv"
```

### Using Swagger UI

1. Navigate to http://localhost:8090 (Centralized Swagger UI) or http://localhost:8081/swagger-ui.html
2. Find the "Bulk receive inventory from CSV" endpoint
3. Click "Try it out"
4. Click "Choose File" and select your CSV file
5. Click "Execute"

### Using Postman

1. Create a POST request to `http://localhost:8081/api/inventory/receive/bulk`
2. Set body type to `form-data`
3. Add a key `file` with type `File`
4. Select your CSV file
5. Send the request

## Sample CSV Files

Sample CSV files are included:

1. **Basic example**: `inventory-service/src/main/resources/sample-inventory.csv`
2. **Comprehensive example**: `docs/sample-inventory-bulk.csv`

You can use these as templates for your own imports.

### Example CSV Content

```csv
inventoryItemId,quantity
laptop-dell-001,25
monitor-hp-002,15
keyboard-logitech-003,50
mouse-microsoft-004,30
```

**Note**: The header row is optional. If the first line contains keywords like "inventoryItemId" or "quantity", it will be automatically detected and skipped.

## Error Handling

The bulk import endpoint handles various error cases:

- **Empty file**: Returns 400 with error message
- **Invalid file format**: Returns 400 if file doesn't have .csv extension
- **Invalid CSV format**: Invalid lines are skipped with warnings logged
- **Invalid quantity**: Lines with non-numeric or negative quantities are skipped
- **Empty item IDs**: Lines with empty item IDs are skipped

The response includes:
- `recordsProcessed`: Number of successfully processed records
- `recordsFailed`: Number of records that failed (currently always 0, as failures are skipped)
- `message`: Status message

## Processing Flow

1. CSV file is uploaded via the API endpoint
2. File is validated (format, extension)
3. CSV is parsed line by line
4. Each valid record is added to a list
5. All records are processed together via the existing `receiveInventory` method
6. An `InventoryReceivedEvent` is published to Kafka
7. Persistence Service processes the event and updates the database

## Best Practices

1. **Validate your CSV** before uploading:
   - Ensure all item IDs are valid
   - Check that quantities are non-negative integers
   - Verify file encoding is UTF-8

2. **Use headers** for clarity:
   ```csv
   inventoryItemId,quantity
   item1,10
   ```

3. **Handle large files**:
   - For very large files (>10,000 records), consider splitting into multiple uploads
   - Monitor Kafka consumer lag if processing many bulk imports

4. **Check logs**:
   - Warnings are logged for skipped lines
   - Check service logs if records aren't appearing as expected

## Troubleshooting

### No records processed

- Check that your CSV file has the correct format
- Verify file encoding is UTF-8
- Check service logs for parsing errors

### Some records skipped

- Review service logs for warnings about specific lines
- Common issues:
  - Empty item IDs
  - Invalid quantity format (non-numeric)
  - Negative quantities

### File upload fails

- Ensure file has `.csv` extension
- Check file size (very large files may timeout)
- Verify Content-Type is `multipart/form-data`

## Integration with Docker

When running in Docker, you can upload CSV files from your host machine:

```bash
# Upload CSV file to containerized service
curl -X POST http://localhost:8081/api/inventory/receive/bulk \
  -F "file=@/path/to/your/inventory.csv"
```

The service will process the file and publish events to Kafka, which will be consumed by the Persistence Service.
