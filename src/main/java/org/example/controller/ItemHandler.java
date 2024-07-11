package org.example.controller;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger log = LoggerFactory.getLogger(ItemHandler.class);
    private final DynamoDB dynamoDB;
    private final Table table;
    private final ObjectMapper objectMapper;

    /**
     * Constructor initializing DynamoDB client and table.
     */
    public ItemHandler() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
        this.objectMapper= new ObjectMapper();
        this.dynamoDB = new DynamoDB(client);
        this.table = dynamoDB.getTable("Item");
    }

    /**
     * Handles the incoming request based on HTTP method to perform CRUD operations on DynamoDB.
     *
     * @param request The API Gateway proxy request event.
     * @param context The Lambda execution context.
     * @return The API Gateway proxy response event indicating the status of the operation.
     */
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {

            System.out.println("Request ID: " + context.getAwsRequestId());
            System.out.println("Function Name: " + context.getFunctionName());

            if (request == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("Request is null");
            }

            String httpMethod = request.getHttpMethod();
            if (httpMethod == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("HTTP method is null");
            }
            return switch (httpMethod) {
                case "POST" -> createItem(request);
                case "GET" -> readItem(request);
                case "PUT" -> updateItem(request);
                case "DELETE" -> deleteItem(request);
                default -> new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Unsupported HTTP method");
            };
        }catch (Exception e){
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("errorMessage: " + e.getMessage());

        }
    }

    /**
     * Creates a new item in DynamoDB based on the incoming request data.
     *
     * @param request The API Gateway proxy request event containing item data.
     * @return The API Gateway proxy response event indicating the status of the create operation.
     */

    private APIGatewayProxyResponseEvent createItem(APIGatewayProxyRequestEvent request) {
        try {
            Map<String, String> input = request.getQueryStringParameters();
            if (input == null || input.isEmpty() || input.get("id") == null) {
                log.error("createItem: Invalid input parameters");
                return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Invalid input parameters");
            }

            String id = input.get("id");
            String name = input.get("name");
            String description = input.get("description");

            if (id == null || name == null || description == null) {
                log.error("createItem: Missing required parameters");
                return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Missing required parameters");
            }

            Item item = new Item()
                    .withPrimaryKey("id", id)
                    .withString("name", name)
                    .withString("description", description);

            table.putItem(item);
            log.warn("createItem: Item created successfully: {}", item);
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("Item created successfully");
        } catch (Exception e) {
            log.error("createItem: Error creating item: " + e.getMessage());
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Error creating item: " + e.getMessage());
        }
    }


    /**
     * Retrieves items from DynamoDB based on the provided item ID or retrieves all items if no ID is specified.
     *
     * @param request The API Gateway proxy request event containing optional query parameters.
     * @return The API Gateway proxy response event containing the retrieved item(s) or an error message.
     */

    private APIGatewayProxyResponseEvent readItem(APIGatewayProxyRequestEvent request) {
        try {
            Map<String, String> queryParams = request.getQueryStringParameters();
            if (queryParams != null && queryParams.containsKey("id")) {
                String id = queryParams.get("id");
                return getItem(id);
            } else {
                return getItem();
            }
        } catch (Exception e) {
            log.error("Error reading items: ", e);
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Error reading item: " + e.getMessage());
        }
    }

    /**
     * Updates an existing item in DynamoDB based on the provided item ID and data.
     *
     * @param request The API Gateway proxy request event containing the updated item data and ID.
     * @return The API Gateway proxy response event indicating the status of the update operation.
     */

    private APIGatewayProxyResponseEvent updateItem(APIGatewayProxyRequestEvent request) {
        Map<String, String> input = request.getQueryStringParameters();
        String itemId = input.get("id");

        try {
            if (!itemExists(itemId)){
                log.error("updateItem: Update item for itemId: {} Not Found:", itemId);
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("Item Not Found");
            }
            Item item = new Item()
                    .withPrimaryKey("id", itemId)
                    .withString("name", input.get("name"))
                    .withString("description", input.get("description"));

            table.putItem(item);
            log.warn("updateItem: Update item for itemId: {} success:", itemId);
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("Item updated successfully");
        } catch (Exception e) {
            log.error("Error updating item: " + e.getMessage());
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Error updating item: " + e.getMessage());
        }
    }

    /**
     * Deletes an item from DynamoDB based on the provided item ID.
     *
     * @param request The API Gateway proxy request event containing the item ID.
     * @return The API Gateway proxy response event indicating the status of the delete operation.
     */

    private APIGatewayProxyResponseEvent deleteItem(APIGatewayProxyRequestEvent request) {
        Map<String, String> input = request.getQueryStringParameters();
        String itemId = input.get("id");

        try {
            if (!itemExists(itemId)){
                log.warn("deleteItem: Delete item for itemId: {} Not Found:", itemId);
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("Item Not Found");
            }
            table.deleteItem("id", itemId);
            log.warn("deleteItem: Delete item for itemId: {} success:", itemId);
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("Item deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting item: " + e.getMessage());
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Error deleting item: " + e.getMessage());
        }
    }

    /**
     * Retrieves an item from DynamoDB based on the provided item ID.
     *
     * @param itemId The ID of the item to retrieve.
     * @return The API Gateway proxy response event containing the retrieved item or an error message if not found.
     */
    private APIGatewayProxyResponseEvent getItem(String itemId) {
        try {
            Item item = table.getItem("id", itemId);
            if (itemExists(itemId)){
                log.warn("readItem: Read item for itemId: {} success:", itemId);
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withBody(item.toJSON());
            } else {
                log.warn("readItem: Read item for itemId: {} Not Found:", itemId);
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("Item Not Found");
            }
        } catch (Exception e) {
            log.error("Error reading item: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Error reading item: " + e.getMessage());
        }
    }

    /**
     * Retrieves all items from DynamoDB.
     *
     * @return The API Gateway proxy response event containing all retrieved items or an error message.
     */
    private APIGatewayProxyResponseEvent getItem() {
        try {
            ScanSpec scanSpec = new ScanSpec();
            ItemCollection<ScanOutcome> items = table.scan(scanSpec);
            List<Map<String, Object>> itemList = new ArrayList<>();
            items.forEach(item -> itemList.add(item.asMap()));
            String responseBody = objectMapper.writeValueAsString(itemList);
            log.warn("readItem: Read items success: {}", itemList);
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withBody(responseBody);
        } catch (Exception e) {
            log.error("Error reading item: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Error reading item: " + e.getMessage());
        }
    }

    /**
     * Checks if an item with the specified item ID exists in DynamoDB.
     *
     * @param itemId The ID of the item to check.
     * @return True if the item exists, false otherwise.
     */
    private boolean itemExists(String itemId) {
        Item item = table.getItem("id", itemId);
        return item != null;
    }
}
