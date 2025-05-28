#!/bin/bash

# Build Lambda function
echo "Building Lambda function..."

cd lambda
mvn clean package

# Copy the JAR file to the expected location for CDK
mkdir -p ../lambda-dist
cp target/lambda-function-1.0.0.jar ../lambda-dist/

echo "Lambda function built successfully!" 