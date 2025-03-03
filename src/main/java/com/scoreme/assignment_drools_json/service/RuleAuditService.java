package com.scoreme.assignment_drools_json.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RuleAuditService {

    private final Map<String, List<RuleExecution>> ruleExecutions = new ConcurrentHashMap<>();

    public static class RuleExecution {
        private String ruleName;
        private Date executionTime;
        private String objectId;
        private Map<String, Object> beforeState;
        private Map<String, Object> afterState;
        private List<String> modifiedProperties;

        public RuleExecution(String ruleName, String objectId) {
            this.ruleName = ruleName;
            this.objectId = objectId;
            this.executionTime = new Date();
            this.beforeState = new ConcurrentHashMap<>();
            this.afterState = new ConcurrentHashMap<>();
            this.modifiedProperties = new ArrayList<>();
        }

        // Getters and setters
        public String getRuleName() { return ruleName; }
        public Date getExecutionTime() { return executionTime; }
        public String getObjectId() { return objectId; }
        public Map<String, Object> getBeforeState() { return beforeState; }
        public Map<String, Object> getAfterState() { return afterState; }
        public List<String> getModifiedProperties() { return modifiedProperties; }

        public void setBeforeState(Map<String, Object> state) {
            this.beforeState = new ConcurrentHashMap<>(state);
        }

        public void setAfterState(Map<String, Object> state) {
            this.afterState = new ConcurrentHashMap<>(state);

            // Calculate modified properties
            for (String key : afterState.keySet()) {
                Object before = beforeState.get(key);
                Object after = afterState.get(key);

                if (!isEqual(before, after)) {
                    modifiedProperties.add(key);
                }
            }
        }

        private boolean isEqual(Object o1, Object o2) {
            if (o1 == null && o2 == null) return true;
            if (o1 == null || o2 == null) return false;
            return o1.equals(o2);
        }
    }

    public void recordRuleExecution(String ruleName, String objectId, Map<String, Object> beforeState) {
        RuleExecution execution = new RuleExecution(ruleName, objectId);
        execution.setBeforeState(beforeState);

        // Store temporarily
        ruleExecutions.computeIfAbsent(objectId, k -> new ArrayList<>()).add(execution);
    }

    public void completeRuleExecution(String objectId, Map<String, Object> afterState) {
        List<RuleExecution> executions = ruleExecutions.get(objectId);
        if (executions != null && !executions.isEmpty()) {
            RuleExecution lastExecution = executions.get(executions.size() - 1);
            lastExecution.setAfterState(afterState);

            // Log the execution
            logRuleExecution(lastExecution);
        }
    }

    private void logRuleExecution(RuleExecution execution) {
        System.out.println("===== RULE EXECUTION AUDIT =====");
        System.out.println("Rule: " + execution.getRuleName());
        System.out.println("Object: " + execution.getObjectId());
        System.out.println("Time: " + execution.getExecutionTime());
        System.out.println("Modified properties: " + execution.getModifiedProperties());

        for (String property : execution.getModifiedProperties()) {
            System.out.println("  " + property + ": " +
                    execution.getBeforeState().get(property) + " -> " +
                    execution.getAfterState().get(property));
        }
        System.out.println("===============================");
    }

    public List<RuleExecution> getExecutionsForObject(String objectId) {
        return ruleExecutions.getOrDefault(objectId, new ArrayList<>());
    }

    public void clearExecutions() {
        ruleExecutions.clear();
    }
}