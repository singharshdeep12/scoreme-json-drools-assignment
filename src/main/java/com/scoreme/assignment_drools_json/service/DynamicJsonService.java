package com.scoreme.assignment_drools_json.service;

import com.google.gson.*;
import com.scoreme.assignment_drools_json.model.DynamicObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DynamicJsonService {

    @Autowired
    private SchemaService schemaService;

    public DynamicObject convertJsonToDynamicObject(String jsonStr) {
        // Parse JSON
        JsonObject jsonObject = new Gson().fromJson(jsonStr, JsonObject.class);

        // Create dynamic object with reference to original JSON
        DynamicObject dynamicObject = new DynamicObject(jsonObject);

        // Recursively flatten the structure for rule processing
        flattenJson(jsonObject, "", dynamicObject);

        return dynamicObject;
    }

    private void flattenJson(JsonObject json, String prefix, DynamicObject dynamicObject) {
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            String path = prefix.isEmpty() ? key : prefix + "_" + key;
            JsonElement value = entry.getValue();

            if (value.isJsonObject()) {
                // Recurse into nested objects
                flattenJson(value.getAsJsonObject(), path, dynamicObject);
            } else if (value.isJsonArray()) {
                // Handle arrays
                handleJsonArray(value.getAsJsonArray(), path, dynamicObject);
            } else if (value.isJsonPrimitive()) {
                // Extract primitive values
                if (value.getAsJsonPrimitive().isNumber()) {
                    dynamicObject.set(path, value.getAsJsonPrimitive().getAsNumber());
                } else if (value.getAsJsonPrimitive().isBoolean()) {
                    dynamicObject.set(path, value.getAsJsonPrimitive().getAsBoolean());
                } else {
                    dynamicObject.set(path, value.getAsJsonPrimitive().getAsString());
                }
            }
        }
    }

    private void handleJsonArray(JsonArray array, String path, DynamicObject dynamicObject) {
        // For arrays of primitives
        List<Object> values = new ArrayList<>();
        // For arrays of objects (to maintain all nested properties)
        List<Map<String, Object>> objectValues = new ArrayList<>();

        boolean containsObjects = false;

        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);

            if (element.isJsonObject()) {
                containsObjects = true;
                // Create a separate index path for each object in the array
                String indexedPath = path + "_" + i;
                flattenJson(element.getAsJsonObject(), indexedPath, dynamicObject);

                // Also store the complete object's properties to maintain the array structure
                Map<String, Object> objectProps = new HashMap<>();
                for (Map.Entry<String, JsonElement> prop : element.getAsJsonObject().entrySet()) {
                    if (prop.getValue().isJsonPrimitive()) {
                        JsonPrimitive primitive = prop.getValue().getAsJsonPrimitive();
                        if (primitive.isNumber()) {
                            objectProps.put(prop.getKey(), primitive.getAsNumber());
                        } else if (primitive.isBoolean()) {
                            objectProps.put(prop.getKey(), primitive.getAsBoolean());
                        } else {
                            objectProps.put(prop.getKey(), primitive.getAsString());
                        }
                    }
                }
                objectValues.add(objectProps);
            } else if (element.isJsonPrimitive()) {
                // Extract primitive values
                if (element.getAsJsonPrimitive().isNumber()) {
                    values.add(element.getAsJsonPrimitive().getAsNumber());
                } else if (element.getAsJsonPrimitive().isBoolean()) {
                    values.add(element.getAsJsonPrimitive().getAsBoolean());
                } else {
                    values.add(element.getAsJsonPrimitive().getAsString());
                }
            }
        }

        // Store the entire array structure
        if (containsObjects) {
            dynamicObject.set(path, objectValues);

            // For specific properties that Drools might need to check across all objects
            extractKeyValues(objectValues, path, dynamicObject);
        } else if (!values.isEmpty()) {
            dynamicObject.set(path, values);
        }
    }

    // Helper method to extract specific keys from all objects in an array
    private void extractKeyValues(List<Map<String, Object>> objects, String basePath, DynamicObject dynamicObject) {
        // Extract common properties that rules might need to check
        // For example, extract all severity values from alerts
        if (basePath.endsWith("alerts")) {
            List<Object> allSeverities = new ArrayList<>();
            for (Map<String, Object> obj : objects) {
                if (obj.containsKey("severity")) {
                    allSeverities.add(obj.get("severity"));
                }
            }
            // Store all severity values so rules can easily check them
            if (!allSeverities.isEmpty()) {
                dynamicObject.set(basePath + "_severity", allSeverities);
            }
        }
    }

    public String convertDynamicObjectToJson(DynamicObject dynamicObject) {
        // Get our original JSON structure
        JsonObject originalJson = dynamicObject.getOriginalJson();

        // Apply the modifications from our dynamic object
        if (dynamicObject.get("customerDetails_extractedParentData") != null) {
            // Find the customerDetails array
            if (originalJson.has("customerDetails") && originalJson.get("customerDetails").isJsonArray()) {
                JsonArray customerDetails = originalJson.get("customerDetails").getAsJsonArray();

                // Add extractedParentData to each customer object
                for (int i = 0; i < customerDetails.size(); i++) {
                    if (customerDetails.get(i).isJsonObject()) {
                        JsonObject customer = customerDetails.get(i).getAsJsonObject();

                        // Add the extractedParentData field
                        customer.addProperty("extractedParentData",
                                dynamicObject.get("customerDetails_extractedParentData").toString());
                    }
                }
            }
        }

        return new Gson().toJson(originalJson);
    }

}