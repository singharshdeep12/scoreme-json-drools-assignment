package com.scoreme.assignment_drools_json.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.scoreme.assignment_drools_json.model.DynamicObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to handle multiple API responses
 */
@Service
public class ApiResponseHandler {

    @Autowired
    private DynamicJsonService jsonService;

    @Autowired
    private DroolsService droolsService;

    // Map to store metadata about different API types
    private final Map<String, ApiMetadata> apiMetadataMap = new HashMap<>();

    public ApiResponseHandler() {
        // Register different API types and their metadata
        registerApiType("customerDetails", "customer");
        registerApiType("weatherInfo", "weather");
        registerApiType("financialData", "financial");
        // Add more API types as needed
    }

    /**
     * Register metadata for a specific API type
     */
    private void registerApiType(String apiName, String objectPrefix) {
        apiMetadataMap.put(apiName, new ApiMetadata(apiName, objectPrefix));
    }

    /**
     * Process a JSON response from a specific API
     * @param jsonResponse The JSON response string
     * @param apiType The type of API (e.g., "customerDetails", "weatherInfo")
     * @return Processed JSON response after rule application
     */
    public String processApiResponse(String jsonResponse, String apiType) {
        try {
            // Get API metadata if available, or use default processing
            ApiMetadata metadata = apiMetadataMap.getOrDefault(apiType,
                    new ApiMetadata(apiType, apiType));

            // Parse JSON
            JsonObject jsonObject = new Gson().fromJson(jsonResponse, JsonObject.class);

            // Create dynamic object with API type info
            DynamicObject dynamicObject = jsonService.convertJsonToDynamicObject(jsonResponse);

            // Add API type information for rule context
            dynamicObject.set("_apiType", apiType);
            dynamicObject.set("_objectPrefix", metadata.getObjectPrefix());

            // Apply rules with context of API type
            droolsService.processRules(dynamicObject);

            // Apply changes back to original structure
            dynamicObject.applyChangesToOriginal();

            // Return modified JSON
            return new Gson().toJson(dynamicObject.getOriginalJson());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error processing API response: " + e.getMessage(), e);
        }
    }

    /**
     * Process multiple API responses together
     * @param apiResponses Map of API type to response JSON
     * @return Map of API type to processed response JSON
     */
    public Map<String, String> processMultipleResponses(Map<String, String> apiResponses) {
        Map<String, String> processedResponses = new HashMap<>();

        // Process each API response
        for (Map.Entry<String, String> entry : apiResponses.entrySet()) {
            String apiType = entry.getKey();
            String response = entry.getValue();

            processedResponses.put(apiType, processApiResponse(response, apiType));
        }

        return processedResponses;
    }

    /**
     * Class to store metadata about API types
     */
    private static class ApiMetadata {
        private final String apiName;
        private final String objectPrefix;

        public ApiMetadata(String apiName, String objectPrefix) {
            this.apiName = apiName;
            this.objectPrefix = objectPrefix;
        }

        public String getApiName() {
            return apiName;
        }

        public String getObjectPrefix() {
            return objectPrefix;
        }
    }
}