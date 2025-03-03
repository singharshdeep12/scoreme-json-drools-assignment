// 1. Enhance DynamicObject to be more flexible
package com.scoreme.assignment_drools_json.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DynamicObject {
    private Map<String, Object> properties = new HashMap<>();
    private JsonObject originalJson;
    private Map<String, String> modifiedPaths = new HashMap<>(); // Track which paths were modified by rules

    public DynamicObject(JsonObject originalJson) {
        this.originalJson = originalJson;
    }

    public void set(String key, Object value) {
        properties.put(key, value);
        modifiedPaths.put(key, "modified"); // Track that this path was modified
    }

    public Object get(String key) {
        return properties.get(key);
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    public Set<String> getPropertyKeys() {
        return properties.keySet();
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public JsonObject getOriginalJson() {
        return originalJson;
    }

    // Enhanced to handle any modification path
    public void applyChangesToOriginal() {
        for (String modifiedPath : modifiedPaths.keySet()) {
            // Parse the path to identify where to apply changes
            String[] pathParts = modifiedPath.split("_");
            String targetProperty = pathParts[pathParts.length - 1];

            // Special case for extractedParentData which we know needs to go to customerDetails
            if ("extractedParentData".equals(targetProperty)) {
                applyToCustomerDetails(targetProperty, properties.get(modifiedPath));
            } else {
                // For other modifications, apply to the appropriate path
                applyToJsonPath(originalJson, pathParts, 0, properties.get(modifiedPath));
            }
        }
    }

    private void applyToCustomerDetails(String property, Object value) {
        if (originalJson.has("customerDetails") && originalJson.get("customerDetails").isJsonArray()) {
            JsonArray customerDetails = originalJson.get("customerDetails").getAsJsonArray();

            for (int i = 0; i < customerDetails.size(); i++) {
                if (customerDetails.get(i).isJsonObject()) {
                    JsonObject customer = customerDetails.get(i).getAsJsonObject();
                    String stringValue = value != null ? value.toString() : "";
                    customer.addProperty(property, stringValue);
                }
            }
        }
    }

    private void applyToJsonPath(JsonObject json, String[] pathParts, int index, Object value) {
        if (index >= pathParts.length) return;

        String part = pathParts[index];

        // If this is the last part of the path, apply the change
        if (index == pathParts.length - 1) {
            if (value instanceof String) {
                json.addProperty(part, (String)value);
            } else if (value instanceof Number) {
                json.addProperty(part, (Number)value);
            } else if (value instanceof Boolean) {
                json.addProperty(part, (Boolean)value);
            }
            return;
        }

        // Navigate to the next level
        if (json.has(part)) {
            JsonElement element = json.get(part);
            if (element.isJsonObject()) {
                applyToJsonPath(element.getAsJsonObject(), pathParts, index + 1, value);
            } else if (element.isJsonArray()) {
                // Handle arrays - assumes next part is an index
                try {
                    int arrayIndex = Integer.parseInt(pathParts[index + 1]);
                    JsonArray array = element.getAsJsonArray();
                    if (arrayIndex >= 0 && arrayIndex < array.size() && array.get(arrayIndex).isJsonObject()) {
                        applyToJsonPath(array.get(arrayIndex).getAsJsonObject(), pathParts, index + 2, value);
                    }
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    // Not a valid index, continue with next part
                }
            }
        }
    }
}