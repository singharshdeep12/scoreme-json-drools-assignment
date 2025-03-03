package com.scoreme.assignment_drools_json.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.scoreme.assignment_drools_json.model.Schema;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class SchemaService {

    // Pattern to check if a string is numeric
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");

    // Improved schema inference with better type detection
    public Schema inferSchemaFromJson(String json) {
        JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
        Schema schema = new Schema();
        schema.setDataObjectName("DynamicObjects");
        schema.setPackage("com.scoreme.assignment_drools_json");

        Schema.DataPoint dataPoint = new Schema.DataPoint();
        dataPoint.setApi("dynamicAPI");

        List<Schema.DataObject> dataObjects = new ArrayList<>();

        // Recursively traverse JSON and create schema objects
        traverseJson(jsonObject, "", dataObjects);

        dataPoint.setDataObjects(dataObjects);

        List<Schema.DataPoint> dataPoints = new ArrayList<>();
        dataPoints.add(dataPoint);

        schema.setDataPoints(dataPoints);

        return schema;
    }

    private void traverseJson(JsonObject json, String prefix, List<Schema.DataObject> dataObjects) {
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            String currentPath = prefix.isEmpty() ? key : prefix + "_" + key;
            JsonElement element = entry.getValue();

            if (element.isJsonObject()) {
                traverseJson(element.getAsJsonObject(), currentPath, dataObjects);
            }
            else if (element.isJsonArray()) {
                handleJsonArray(element.getAsJsonArray(), currentPath, dataObjects);
            }
            else {
                // This is a primitive value with improved type inference
                String dataType = inferDataTypeAdvanced(element);
                dataObjects.add(createDataObject(dataType, currentPath));
            }
        }
    }

    private void handleJsonArray(JsonArray array, String path, List<Schema.DataObject> dataObjects) {
        if (array.size() == 0) {
            // Empty array - add a generic entry
            dataObjects.add(createDataObject("Object[]", path));
            return;
        }

        // Check if array is homogeneous (all elements same type)
        String elementType = null;
        boolean mixedTypes = false;

        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);

            if (element.isJsonObject()) {
                // Process object in array
                traverseJson(element.getAsJsonObject(), path + "_" + i, dataObjects);

                // Also add array entry
                if (elementType == null) {
                    elementType = "Object";
                } else if (!elementType.equals("Object")) {
                    mixedTypes = true;
                }
            } else if (element.isJsonArray()) {
                // Nested array
                handleJsonArray(element.getAsJsonArray(), path + "_" + i, dataObjects);

                if (elementType == null) {
                    elementType = "Array";
                } else if (!elementType.equals("Array")) {
                    mixedTypes = true;
                }
            } else {
                // Primitive type
                String currentType = inferDataTypeAdvanced(element);

                if (elementType == null) {
                    elementType = currentType;
                } else if (!elementType.equals(currentType)) {
                    mixedTypes = true;
                }
            }
        }

        // Add array type entry
        String arrayType = mixedTypes ? "Object[]" : elementType + "[]";
        dataObjects.add(createDataObject(arrayType, path));
    }

    private String inferDataTypeAdvanced(JsonElement element) {
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isBoolean()) {
                return "Boolean";
            } else if (element.getAsJsonPrimitive().isNumber()) {
                // Check if it's an integer or decimal
                String numStr = element.getAsString();
                if (numStr.contains(".")) {
                    return "Double";
                } else {
                    // Check range to determine if it's an Integer or Long
                    try {
                        Integer.parseInt(numStr);
                        return "Integer";
                    } catch (NumberFormatException e) {
                        return "Long";
                    }
                }
            } else {
                // String type - but check if it might be a date
                String strValue = element.getAsString();
                if (isDateFormat(strValue)) {
                    return "Date";
                } else if (NUMERIC_PATTERN.matcher(strValue).matches()) {
                    // It's a number stored as a string, should be converted to Number
                    return "Number";
                }
                return "String";
            }
        }
        return "Object";
    }

    private boolean isDateFormat(String str) {
        // Simple check for common date formats (could be expanded)
        return str.matches("\\d{4}-\\d{2}-\\d{2}.*") || // ISO date
                str.matches("\\d{2}/\\d{2}/\\d{4}.*") || // MM/DD/YYYY
                str.matches("\\d{2}-\\d{2}-\\d{4}.*");   // MM-DD-YYYY
    }

    // Helper method to create a data object with proper imports
    private Schema.DataObject createDataObject(String dataType, String keyName) {
        Schema.DataObject dataObject = new Schema.DataObject();
        dataObject.setDataType(dataType);
        dataObject.setKeyName(keyName);

        // Set appropriate imports based on data type
        List<String> imports = new ArrayList<>();

        if (dataType.equals("Date")) {
            imports.add("java.util.Date");
        } else if (dataType.contains("[]")) {
            imports.add("java.util.List");
            imports.add("java.util.ArrayList");
        }

        dataObject.setImports(imports.isEmpty() ? Arrays.asList("") : imports);
        return dataObject;
    }
}