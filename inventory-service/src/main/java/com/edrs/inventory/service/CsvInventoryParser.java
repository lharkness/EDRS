package com.edrs.inventory.service;

import com.edrs.inventory.dto.InventoryReceiveRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvInventoryParser {
    private static final Logger logger = LoggerFactory.getLogger(CsvInventoryParser.class);
    
    private static final String CSV_CONTENT_TYPE = "text/csv";
    private static final String CSV_EXTENSION = ".csv";
    
    /**
     * Parses a CSV file containing inventory receive records.
     * Expected format: inventoryItemId,quantity
     * First line is treated as header and skipped.
     * 
     * @param file The CSV file to parse
     * @return List of receive records
     * @throws IllegalArgumentException if file format is invalid
     */
    public List<InventoryReceiveRequest.ReceiveRecord> parseCsv(MultipartFile file) {
        validateFile(file);
        
        List<InventoryReceiveRequest.ReceiveRecord> records = new ArrayList<>();
        int lineNumber = 0;
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            String line;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }
                
                // Skip header line (first non-empty line)
                if (isFirstLine) {
                    isFirstLine = false;
                    // Check if it's a header (contains "inventoryItemId" or "quantity")
                    if (line.toLowerCase().contains("inventoryitemid") || 
                        line.toLowerCase().contains("quantity") ||
                        line.toLowerCase().contains("item") ||
                        line.toLowerCase().contains("id")) {
                        logger.debug("Skipping header line: {}", line);
                        continue;
                    }
                }
                
                // Parse CSV line
                String[] parts = parseCsvLine(line);
                
                if (parts.length < 2) {
                    logger.warn("Skipping invalid line {}: expected 2 columns, found {}", lineNumber, parts.length);
                    continue;
                }
                
                try {
                    String inventoryItemId = parts[0].trim();
                    int quantity = Integer.parseInt(parts[1].trim());
                    
                    if (inventoryItemId.isEmpty()) {
                        logger.warn("Skipping line {}: empty inventory item ID", lineNumber);
                        continue;
                    }
                    
                    if (quantity < 0) {
                        logger.warn("Skipping line {}: negative quantity not allowed", lineNumber);
                        continue;
                    }
                    
                    InventoryReceiveRequest.ReceiveRecord record = new InventoryReceiveRequest.ReceiveRecord();
                    record.setInventoryItemId(inventoryItemId);
                    record.setQuantity(quantity);
                    records.add(record);
                    logger.debug("Parsed record: itemId={}, quantity={}", inventoryItemId, quantity);
                    
                } catch (NumberFormatException e) {
                    logger.warn("Skipping line {}: invalid quantity format '{}'", lineNumber, parts[1]);
                }
            }
            
            logger.info("Successfully parsed {} records from CSV file", records.size());
            return records;
            
        } catch (Exception e) {
            logger.error("Error parsing CSV file at line {}", lineNumber, e);
            throw new IllegalArgumentException("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses a CSV line, handling quoted fields.
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        
        // Add the last field
        fields.add(currentField.toString());
        
        return fields.toArray(new String[0]);
    }
    
    /**
     * Validates that the uploaded file is a CSV file.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required and cannot be empty");
        }
        
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        
        // Check content type
        if (contentType != null && !contentType.equals(CSV_CONTENT_TYPE) && 
            !contentType.equals("application/vnd.ms-excel") &&
            !contentType.equals("application/csv")) {
            logger.warn("Unexpected content type: {} for file: {}", contentType, filename);
        }
        
        // Check file extension
        if (filename == null || !filename.toLowerCase().endsWith(CSV_EXTENSION)) {
            throw new IllegalArgumentException("File must be a CSV file (.csv extension required)");
        }
    }
}
