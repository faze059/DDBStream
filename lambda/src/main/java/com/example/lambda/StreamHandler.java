package com.example.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Lambda function to process DynamoDB Stream events
 * Writes new records from TableA to TableB
 */
public class StreamHandler implements RequestHandler<DynamodbEvent, String> {
    
    private final DynamoDbClient dynamoDbClient;
    private final String tableBName;
    private final ObjectMapper objectMapper;
    
    public StreamHandler() {
        this.dynamoDbClient = DynamoDbClient.create();
        this.tableBName = System.getenv("TABLE_B_NAME");
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String handleRequest(DynamodbEvent event, Context context) {
        context.getLogger().log("Processing " + event.getRecords().size() + " records from DynamoDB Stream");
        
        int processedCount = 0;
        
        for (DynamodbEvent.DynamodbStreamRecord record : event.getRecords()) {
            try {
                // Only process INSERT and MODIFY events
                if ("INSERT".equals(record.getEventName()) || "MODIFY".equals(record.getEventName())) {
                    processRecord(record, context);
                    processedCount++;
                }
            } catch (Exception e) {
                context.getLogger().log("Error processing record: " + e.getMessage());
                // In production, you might want to send failed records to a DLQ
            }
        }
        
        String result = "Successfully processed " + processedCount + " records";
        context.getLogger().log(result);
        return result;
    }
    
    private void processRecord(DynamodbEvent.DynamodbStreamRecord record, Context context) {
        // Get the new image (the record after it was modified)
        Map<String, com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue> newImage = 
            record.getDynamodb().getNewImage();
        
        if (newImage == null || newImage.isEmpty()) {
            context.getLogger().log("No new image found in record, skipping");
            return;
        }
        
        // Convert the record to be written to TableB
        Map<String, AttributeValue> item = new HashMap<>();
        
        // Copy the original ID
        if (newImage.containsKey("id")) {
            item.put("id", AttributeValue.builder()
                    .s(newImage.get("id").getS())
                    .build());
        }
        
        // Add processed timestamp
        item.put("processedAt", AttributeValue.builder()
                .s(Instant.now().toString())
                .build());
        
        // Add event type
        item.put("eventType", AttributeValue.builder()
                .s(record.getEventName())
                .build());
        
        // Copy other attributes from the original record
        for (Map.Entry<String, com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue> entry : newImage.entrySet()) {
            if (!"id".equals(entry.getKey())) {  // Skip id as we already added it
                String key = "original_" + entry.getKey();
                
                // Convert different attribute types
                com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue value = entry.getValue();
                if (value.getS() != null) {
                    item.put(key, AttributeValue.builder().s(value.getS()).build());
                } else if (value.getN() != null) {
                    item.put(key, AttributeValue.builder().n(value.getN()).build());
                } else if (value.getBOOL() != null) {
                    item.put(key, AttributeValue.builder().bool(value.getBOOL()).build());
                }
            }
        }
        
        // Write to TableB
        PutItemRequest putRequest = PutItemRequest.builder()
                .tableName(tableBName)
                .item(item)
                .build();
        
        dynamoDbClient.putItem(putRequest);
        
        context.getLogger().log("Successfully wrote record to TableB with ID: " + 
                (item.containsKey("id") ? item.get("id").s() : "unknown"));
    }
} 