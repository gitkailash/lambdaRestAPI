# Complete CRUD Operations for Serverless API

## Summary

This technical design documentation (TDD) outlines the process of creating an AWS-based solution for managing items in DynamoDB via Lambda and API Gateway, using Java and Postman for development and testing. The `ItemHandler` class implements a Lambda function to perform CRUD operations on items stored in DynamoDB through API Gateway endpoints.

## Architecture Diagram

### Lambda Flows

1. Client sends HTTP request.
2. API Gateway receives request and triggers Lambda Function.
3. Lambda accesses DynamoDB to perform CRUD operations.
4. Response flows back through API Gateway to the Client.

![Architecture Diagram](https://github.com/gitkailash/lambdaRestAPI/blob/master/src/main/resources/lambda%20flow.png)

## DynamoDB Table Creation

### Steps

1. **Login to AWS Console:**
    - Navigate to DynamoDB service.

2. **Create Table:**
    - Table Name: `Item`
    - Primary Key: `id` (String)

3. **Provisioning:**
    - Use default settings for demonstration (Provisioned mode).

## Lambda Function Creation

### Steps

1. **Login to AWS Console:**
    - Navigate to Lambda service.

2. **Create Lambda Function:**
    - Author from scratch:
        - Function Name: `ItemHandler`
        - Runtime: Java 17
        - Role: Use an existing role with DynamoDB access permissions. If default is set, create a role and add DynamoDB access permission to it.

3. **Navigate to newly created Lambda (`ItemHandler`):**
    - Select code source > upload from:
        - `.zip` or `.jar` file
        - Amazon S3 location
    - Edit Runtime Setting:
        - Add Handler Info
        - Syntax: `packageName.ClassName::functionName` (e.g., `org.example.controller.ItemHandler::handleRequest`)
        - Save it

4. **Environment Variables:**
    - Configure any necessary environment variables (if applicable).

5. **Save and Deploy:**
    - Deploy the Lambda function.

## API Gateway Setup

### Steps

1. **Login to AWS Console:**
    - Navigate to API Gateway service.

2. **Create REST API:**
    - Choose REST API type.
    - API Name: `ItemAPI`

3. **Resource and Method Setup:**
    - **Resource Creation:**
        - Create a resource named `/item`.
    - **Method Creation for Resource `/item`:**
        - Create Method
        - Select GET/POST/PUT/DELETE
        - Select Lambda Function that we created earlier (e.g., `arn:aws:lambda:us-east-1:239XXXX60241:function:ItemHandler`)
        - Repeat this for all HTTP Methods
        - Note: Do not forget to check Lambda proxy integration. This sends the request to your Lambda function as a structured event.

4. **Deploy API:**
    - Deploy the API to a stage (e.g., `dev`, `prod`).
    - Copy Invoke URL (e.g., `https://d3ncxxco50.execute-api.us-east-1.amazonaws.com/dev`)
    - Add `/item` to the Invoke URL (e.g., `https://d3ncxxco50.execute-api.us-east-1.amazonaws.com/dev/item`)

## Usage

### Testing with Postman

- **POST /item:**
    - Add a new item.
- **GET /item:**
    - Retrieve all items.
- **GET /item?id={id}:**
    - Retrieve a specific item by ID.
- **PUT /item:**
    - Update an existing item.
- **DELETE /item?id={id}:**
    - Delete an item by ID.

### Example Requests

1. **POST Request:**

   ```json
   {
     "id": "1",
     "name": "Item1",
     "description": "This is item 1"
   }
   ```

2. **GET Request:**

   ```sh
   GET /item?id=1
   ```

3. **PUT Request:**

   ```json
   {
     "id": "1",
     "name": "UpdatedItem1",
     "description": "This is the updated item 1"
   }
   ```

4. **DELETE Request:**

   ```sh
   DELETE /item?id=1
   ```

## Conclusion

This README provides a comprehensive guide for setting up a serverless API using AWS Lambda, DynamoDB, and API Gateway, with Java for development and Postman for testing. Follow the steps carefully to ensure a successful implementation.
