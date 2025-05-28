#!/bin/bash

echo "Testing DynamoDB Stream deployment..."

# Test adding a record to TableA
echo "Adding test record to TableA..."
aws dynamodb put-item \
    --table-name TableA \
    --item '{
        "id": {"S": "test-'$(date +%s)'"},
        "name": {"S": "Test Item"},
        "description": {"S": "This is a test record"},
        "value": {"N": "42"},
        "active": {"BOOL": true}
    }'

if [ $? -eq 0 ]; then
    echo "✅ Successfully added record to TableA"
else
    echo "❌ Failed to add record to TableA"
    exit 1
fi

# Wait a moment for the Lambda to process
echo "Waiting 10 seconds for Lambda processing..."
sleep 10

# Check TableB for processed records
echo "Checking TableB for processed records..."
aws dynamodb scan --table-name TableB --max-items 5

echo "Test completed!" 