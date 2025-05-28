package com.example;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

/**
 * Main CDK application entry point
 */
public class DdbStreamApp {
    public static void main(final String[] args) {
        App app = new App();

        // Create the stack with environment configuration
        new DdbStreamStack(app, "DdbStreamStack", StackProps.builder()
                .env(Environment.builder()
                        .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                        .region(System.getenv("CDK_DEFAULT_REGION"))
                        .build())
                .build());

        app.synth();
    }
} 