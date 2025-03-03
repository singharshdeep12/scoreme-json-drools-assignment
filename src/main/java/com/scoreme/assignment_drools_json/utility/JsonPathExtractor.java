package com.scoreme.assignment_drools_json.utility;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

public class JsonPathExtractor {

    private static Object extractValueRecursive(JsonElement element, String[] pathParts, int index) {
        if (index >= pathParts.length) {
            return null;
        }

        String currentPath = pathParts[index];

        // Handle final property
        if (index == pathParts.length - 1) {
            if (element.isJsonObject()) {
                JsonObject jsonObject = element.getAsJsonObject();
                if (jsonObject.has(currentPath)) {
                    JsonElement value = jsonObject.get(currentPath);
                    if (value.isJsonPrimitive()) {
                        JsonPrimitive primitive = value.getAsJsonPrimitive();
                        if (primitive.isString()) return primitive.getAsString();
                        if (primitive.isNumber()) return primitive.getAsNumber();
                        if (primitive.isBoolean()) return primitive.getAsBoolean();
                        return primitive.toString();
                    }
                    return value;
                }
            } else if (element.isJsonArray()) {
                // Handle arrays - return a list of values
                JsonArray array = element.getAsJsonArray();
                List<Object> results = new ArrayList<>();

                for (JsonElement item : array) {
                    if (item.isJsonObject() && item.getAsJsonObject().has(currentPath)) {
                        JsonElement value = item.getAsJsonObject().get(currentPath);
                        if (value.isJsonPrimitive()) {
                            results.add(convertPrimitive(value.getAsJsonPrimitive()));
                        } else {
                            results.add(value);
                        }
                    }
                }
                return results;
            }
        } else {
            // Handle nested object
            if (element.isJsonObject()) {
                JsonObject jsonObject = element.getAsJsonObject();
                if (jsonObject.has(currentPath)) {
                    return extractValueRecursive(jsonObject.get(currentPath), pathParts, index + 1);
                }
            }
            // Handle array of objects
            else if (element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                List<Object> results = new ArrayList<>();

                for (JsonElement item : array) {
                    if (item.isJsonObject()) {
                        Object result = extractValueRecursive(item, pathParts, index);
                        if (result != null) {
                            if (result instanceof List) {
                                results.addAll((List<?>) result);
                            } else {
                                results.add(result);
                            }
                        }
                    }
                }
                return results.isEmpty() ? null : results;
            }
        }

        return null;
    }

    private static Object convertPrimitive(JsonPrimitive primitive) {
        if (primitive.isString()) return primitive.getAsString();
        if (primitive.isNumber()) return primitive.getAsNumber();
        if (primitive.isBoolean()) return primitive.getAsBoolean();
        return primitive.toString();
    }

    public static Object extractValue(JsonObject json, String path) {
        String[] parts = path.split("_");
        JsonElement currentElement = json;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            if (currentElement.isJsonObject()) {
                JsonObject obj = currentElement.getAsJsonObject();
                if (obj.has(part)) {
                    currentElement = obj.get(part);
                } else {
                    return null; // Path doesn't exist
                }
            } else if (currentElement.isJsonArray()) {
                // For simplicity, we'll just take the first element that matches
                // A more complete solution would handle arrays properly
                JsonArray array = currentElement.getAsJsonArray();

                // If this is the last part of the path, we might need to extract values from all array elements
                if (i == parts.length - 1) {
                    List<Object> values = new ArrayList<>();
                    for (JsonElement elem : array) {
                        if (elem.isJsonObject() && elem.getAsJsonObject().has(part)) {
                            values.add(getValueFromJsonElement(elem.getAsJsonObject().get(part)));
                        }
                    }
                    return values.isEmpty() ? null : values.size() == 1 ? values.get(0) : values;
                }

                // Otherwise, we're navigating through the structure
                boolean found = false;
                for (JsonElement elem : array) {
                    if (elem.isJsonObject() && elem.getAsJsonObject().has(part)) {
                        currentElement = elem.getAsJsonObject().get(part);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    return null; // Path doesn't exist in any array element
                }
            } else {
                // We've hit a primitive but still have path parts to process
                return null;
            }
        }

        return getValueFromJsonElement(currentElement);
    }

    private static Object getValueFromJsonElement(JsonElement element) {
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) return primitive.getAsString();
            if (primitive.isNumber()) return primitive.getAsNumber();
            if (primitive.isBoolean()) return primitive.getAsBoolean();
        } else if (element.isJsonArray()) {
            List<Object> result = new ArrayList<>();
            for (JsonElement e : element.getAsJsonArray()) {
                result.add(getValueFromJsonElement(e));
            }
            return result;
        } else if (element.isJsonObject()) {
            // Simplified handling of objects - in a real implementation,
            // you'd create a Map or custom object
            return element.toString();
        }

        return null;
    }
}