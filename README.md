# DynamoDB Stream Demo with AWS CDK

This is a demonstration project using AWS CDK (Java) that shows how to:

1. Create two DynamoDB tables (TableA and TableB)
2. Enable DynamoDB Stream on TableA
3. Create a Lambda function to process Stream events
4. Write new records from TableA to TableB

## Project Structure

```
├── src/main/java/com/example/
│   ├── DdbStreamApp.java          # CDK application entry point
│   └── DdbStreamStack.java        # CDK Stack definition
├── lambda/
│   ├── pom.xml                    # Lambda function Maven configuration
│   └── src/main/java/com/example/lambda/
│       └── StreamHandler.java     # Lambda handler function
├── pom.xml                        # Main project Maven configuration
├── cdk.json                       # CDK configuration
├── build-lambda.sh               # Lambda build script
└── README.md                      # Project documentation
```

## Features

### TableA
- Primary key: `id` (String)
- DynamoDB Stream enabled (NEW_AND_OLD_IMAGES)
- On-demand billing mode

### TableB  
- Primary key: `id` (String)
- Stores processed records from TableA
- On-demand billing mode

### Lambda Function
- Listens to TableA Stream events
- Processes INSERT and MODIFY events
- Writes records to TableB with additional metadata:
  - `processedAt`: Processing timestamp
  - `eventType`: Event type (INSERT/MODIFY)
  - `original_*`: Original record attributes (with prefix)

## Deployment Steps

### 1. Prerequisites

Make sure you have installed:
- Java 11+
- Maven 3.6+
- AWS CDK CLI
- AWS CLI (with configured credentials)

### 2. Build Lambda Function

```bash
chmod +x build-lambda.sh
./build-lambda.sh
```

### 3. Deploy CDK Stack

```bash
# Bootstrap CDK (first time only)
cdk bootstrap

# Deploy the stack
cdk deploy
```

### 4. Testing

After deployment, you can add records to TableA via AWS Console or CLI:

```bash
aws dynamodb put-item \
    --table-name TableA \
    --item '{
        "id": {"S": "test-1"},
        "name": {"S": "Test Item"},
        "value": {"N": "100"}
    }'
```

Then check if TableB received the processed record:

```bash
aws dynamodb scan --table-name TableB
```

## Cleanup

```bash
cdk destroy
```

## Important Notes

1. This is a demonstration project. For production environments, consider:
   - Error handling and retry mechanisms
   - Dead Letter Queue (DLQ)
   - Monitoring and alerting
   - Cost optimization

2. Lambda function uses Java 11 runtime
3. DynamoDB tables use on-demand billing mode
4. Stream is configured to capture new and old images

## Architecture Diagram

```
TableA (with Stream) → Lambda Function → TableB
```

When TableA has new records or updates, the Stream triggers the Lambda function, which processes these events and writes the results to TableB. 