package com.scoreme.assignment_drools_json.service;

import com.google.gson.*;
import com.scoreme.assignment_drools_json.model.DynamicObject;
import com.scoreme.assignment_drools_json.model.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SchemaBasedJsonService {

    @Autowired
    private SchemaService schemaService;

    /**
     * Convert JSON to DynamicObject using schema-based approach
     */
    public DynamicObject convertJsonWithSchema(String jsonStr) {
        // Parse JSON
        JsonObject jsonObject = new Gson().fromJson(jsonStr, JsonObject.class);

        // Infer schema from JSON
        Schema schema = schemaService.inferSchemaFromJson(jsonStr);

        // Create dynamic object with reference to original JSON
        DynamicObject dynamicObject = new DynamicObject(jsonObject);

        // Add schema information to dynamic object
        dynamicObject.set("_schema_dataObjectName", schema.getDataObjectName());
        dynamicObject.set("_schema_package", schema.getPackage());

        // Process data points from schema
        processDataPoints(schema, jsonObject, dynamicObject);

        // Flatten JSON as before for compatibility
        flattenJsonWithSchema(jsonObject, "", dynamicObject, schema);

        return dynamicObject;
    }

    /**
     * Process data points from schema and apply to dynamic object
     */
    private void processDataPoints(Schema schema, JsonObject jsonObject, DynamicObject dynamicObject) {
        for (Schema.DataPoint dataPoint : schema.getDataPoints()) {
            String api = dataPoint.getApi();
            dynamicObject.set("_api_" + api, true);

            for (Schema.DataObject dataObj : dataPoint.getDataObjects()) {
                String keyName = dataObj.getKeyName();
                String dataType = dataObj.getDataType();

                // Extract value from JSON using key path
                Object value = extractValueFromJsonPath(jsonObject, keyName);

                if (value != null) {
                    // Apply type conversion if needed
                    Object convertedValue = convertValueToType(value, dataType);
                    dynamicObject.set(keyName, convertedValue);

                    // Store type information
                    dynamicObject.set("_type_" + keyName, dataType);
                }
            }
        }
    }

    /**
     * Extract value from JSON using path notation (e.g., "customer_details_name")
     */
    private Object extractValueFromJsonPath(JsonObject json, String path) {
        String[] parts = path.split("_");
        JsonElement currentElement = json;

        for (String part : parts) {
            if (currentElement == null || !currentElement.isJsonObject()) {
                return null;
            }

            JsonObject currentObject = currentElement.getAsJsonObject();
            if (!currentObject.has(part)) {
                return null;
            }

            currentElement = currentObject.get(part);
        }

        // Convert to appropriate Java type
        return convertJsonElementToObject(currentElement);
    }

    /**
     * Convert JsonElement to appropriate Java object
     */
    private Object convertJsonElementToObject(JsonElement element) {
        if (element == null) {
            return null;
        }

        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) {
                return primitive.getAsString();
            } else if (primitive.isNumber()) {
                return primitive.getAsNumber();
            } else if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            }
        } else if (element.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                list.add(convertJsonElementToObject(item));
            }
            return list;
        } else if (element.isJsonObject()) {
            Map<String, Object> map = new HashMap<>();
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                map.put(entry.getKey(), convertJsonElementToObject(entry.getValue()));
            }
            return map;
        }

        return null;
    }

    /**
     * Convert value to specified data type
     */
    private Object convertValueToType(Object value, String dataType) {
        if (value == null) {
            return null;
        }

        try {
            switch (dataType) {
                case "String":
                    return value.toString();
                case "Integer":
                    if (value instanceof Number) {
                        return ((Number) value).intValue();
                    } else {
                        return Integer.parseInt(value.toString());
                    }
                case "Double":
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    } else {
                        return Double.parseDouble(value.toString());
                    }
                case "Boolean":
                    if (value instanceof Boolean) {
                        return value;
                    } else {
                        return Boolean.parseBoolean(value.toString());
                    }
                default:
                    return value;
            }
        } catch (Exception e) {
            // If conversion fails, return original value
            return value;
        }
    }

    /**
     * Flatten JSON structure with schema awareness
     */
    private void flattenJsonWithSchema(JsonObject json, String prefix, DynamicObject dynamicObject, Schema schema) {
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            String path = prefix.isEmpty() ? key : prefix + "_" + key;
            JsonElement value = entry.getValue();

            // Find data type from schema if available
            String dataType = findDataTypeInSchema(schema, path);

            if (value.isJsonObject()) {
                // Recurse into nested objects
                flattenJsonWithSchema(value.getAsJsonObject(), path, dynamicObject, schema);
            } else if (value.isJsonArray()) {
                // Handle arrays
                handleJsonArrayWithSchema(value.getAsJsonArray(), path, dynamicObject, schema);
            } else if (value.isJsonPrimitive()) {
                // Set value with appropriate type
                setValueWithDataType(dynamicObject, path, value.getAsJsonPrimitive(), dataType);
            }
        }
    }

    /**
     * Find data type in schema for a given path
     */
    private String findDataTypeInSchema(Schema schema, String path) {
        for (Schema.DataPoint dataPoint : schema.getDataPoints()) {
            for (Schema.DataObject dataObj : dataPoint.getDataObjects()) {
                if (dataObj.getKeyName().equals(path)) {
                    return dataObj.getDataType();
                }
            }
        }
        return null; // Not found in schema
    }

    /**
     * Set value in dynamic object with appropriate data type
     */
    private void setValueWithDataType(DynamicObject dynamicObject, String path,
                                      JsonPrimitive primitive, String dataType) {
        if (dataType != null) {
            // Use schema-defined data type
            Object convertedValue = convertJsonPrimitiveToType(primitive, dataType);
            dynamicObject.set(path, convertedValue);
        } else {
            // Infer type
            if (primitive.isNumber()) {
                dynamicObject.set(path, primitive.getAsNumber());
            } else if (primitive.isBoolean()) {
                dynamicObject.set(path, primitive.getAsBoolean());
            } else {
                dynamicObject.set(path, primitive.getAsString());
            }
        }
    }

    /**
     * Convert JSON primitive to specified data type
     */
    private Object convertJsonPrimitiveToType(JsonPrimitive primitive, String dataType) {
        try {
            switch (dataType) {
                case "Integer":
                    return primitive.getAsInt();
                case "Long":
                    return primitive.getAsLong();
                case "Double":
                    return primitive.getAsDouble();
                case "Boolean":
                    return primitive.getAsBoolean();
                case "Date":
                    // Simplified date handling - in a real app you'd use a proper date parser
                    return primitive.getAsString();
                default:
                    return primitive.getAsString();
            }
        } catch (Exception e) {
            // If conversion fails, return as string
            return primitive.getAsString();
        }
    }

    /**
     * Handle JSON array with schema awareness
     */
    private void handleJsonArrayWithSchema(JsonArray array, String path,
                                           DynamicObject dynamicObject, Schema schema) {
        List<Object> values = new ArrayList<>();
        List<Map<String, Object>> objectValues = new ArrayList<>();
        boolean containsObjects = false;

        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);

            if (element.isJsonObject()) {
                containsObjects = true;
                // Create a separate index path for each object in the array
                String indexedPath = path + "_" + i;
                flattenJsonWithSchema(element.getAsJsonObject(), indexedPath, dynamicObject, schema);

                // Also store complete object properties
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
                // Extract primitive values with data type awareness
                String arrayType = findArrayElementType(schema, path);
                values.add(convertJsonPrimitiveToType(element.getAsJsonPrimitive(), arrayType));
            }
        }

        // Store the entire array structure
        if (containsObjects) {
            dynamicObject.set(path, objectValues);
            // Extract key values for rules
            extractKeyValues(objectValues, path, dynamicObject);
        } else if (!values.isEmpty()) {
            dynamicObject.set(path, values);
        }
    }

    /**
     * Find element type for array elements from schema
     */
    private String findArrayElementType(Schema schema, String path) {
        String dataType = findDataTypeInSchema(schema, path);
        if (dataType != null && dataType.endsWith("[]")) {
            return dataType.substring(0, dataType.length() - 2);
        }
        return null;
    }

    /**
     * Extract key values from objects (same as original implementation)
     */
    private void extractKeyValues(List<Map<String, Object>> objects, String basePath, DynamicObject dynamicObject) {
        // Same implementation as in DynamicJsonService
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
}