package com.example;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.eventsources.DynamoEventSource;
import software.amazon.awscdk.services.lambda.eventsources.DynamoEventSourceProps;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Effect;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

/**
 * CDK Stack containing DynamoDB tables with stream and Lambda function
 */
public class DdbStreamStack extends Stack {
    
    public DdbStreamStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public DdbStreamStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Create TableA with DynamoDB Stream enabled
        Table tableA = Table.Builder.create(this, "TableA")
                .tableName("TableA")
                .partitionKey(Attribute.builder()
                        .name("id")
                        .type(AttributeType.STRING)
                        .build())
                .stream(StreamViewType.NEW_AND_OLD_IMAGES) 
                .billingMode(BillingMode.PAY_PER_REQUEST)  
                .build();

        // Create TableB to store processed records
        Table tableB = Table.Builder.create(this, "TableB")
                .tableName("TableB")
                .partitionKey(Attribute.builder()
                        .name("id")
                        .type(AttributeType.STRING)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        // Create Lambda function to process DynamoDB stream records
        Function streamProcessor = Function.Builder.create(this, "StreamProcessor")
                .functionName("ddb-stream-processor")
                .runtime(software.amazon.awscdk.services.lambda.Runtime.JAVA_11)
                .handler("com.example.lambda.StreamHandler::handleRequest")
                .code(Code.fromAsset("lambda-dist/lambda-function-1.0.0.jar"))  
                .timeout(Duration.minutes(5))
                .memorySize(512)
                .environment(Map.of(
                        "TABLE_B_NAME", tableB.getTableName()
                ))
                .build();

        // Grant Lambda permission to write to TableB
        tableB.grantWriteData(streamProcessor);

        // Add DynamoDB stream as event source for Lambda
        streamProcessor.addEventSource(DynamoEventSource.Builder.create(tableA)
                .startingPosition(StartingPosition.LATEST)
                .batchSize(10)
                .maxBatchingWindow(Duration.seconds(5))
                .retryAttempts(3)
                .build());

        // Grant Lambda permission to read from DynamoDB stream
        streamProcessor.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of(
                        "dynamodb:DescribeStream",
                        "dynamodb:GetRecords",
                        "dynamodb:GetShardIterator",
                        "dynamodb:ListStreams"
                ))
                .resources(List.of(tableA.getTableStreamArn()))
                .build());
    }
} 