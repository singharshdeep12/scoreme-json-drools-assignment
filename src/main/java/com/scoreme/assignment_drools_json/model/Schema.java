package com.scoreme.assignment_drools_json.model;

import java.util.List;

public class Schema {
    private String dataObjectName;
    private String packageName;
    private List<DataPoint> dataPoints;

    // Getters and setters
    public String getDataObjectName() {
        return dataObjectName;
    }

    public void setDataObjectName(String dataObjectName) {
        this.dataObjectName = dataObjectName;
    }

    public String getPackage() {
        return packageName;
    }

    public void setPackage(String packageName) {
        this.packageName = packageName;
    }

    public List<DataPoint> getDataPoints() {
        return dataPoints;
    }

    public void setDataPoints(List<DataPoint> dataPoints) {
        this.dataPoints = dataPoints;
    }

    // Inner classes for schema structure
    public static class DataPoint {
        private List<DataObject> dataObjects;
        private String api;

        public List<DataObject> getDataObjects() {
            return dataObjects;
        }

        public void setDataObjects(List<DataObject> dataObjects) {
            this.dataObjects = dataObjects;
        }

        public String getApi() {
            return api;
        }

        public void setApi(String api) {
            this.api = api;
        }
    }

    public static class DataObject {
        private List<String> imports;
        private String dataType;
        private String keyName;

        public List<String> getImports() {
            return imports;
        }

        public void setImports(List<String> imports) {
            this.imports = imports;
        }

        public String getDataType() {
            return dataType;
        }

        public void setDataType(String dataType) {
            this.dataType = dataType;
        }

        public String getKeyName() {
            return keyName;
        }

        public void setKeyName(String keyName) {
            this.keyName = keyName;
        }
    }
}