package com.scoreme.assignment_drools_json.service;

import com.scoreme.assignment_drools_json.model.DynamicObject;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class DroolsService {

    @Autowired
    private KieContainer kieContainer;

    @Autowired
    private RuleAuditService auditService;

    public DynamicObject processRules(DynamicObject dynamicObject) {
        KieSession kieSession = kieContainer.newKieSession();
        String objectId = UUID.randomUUID().toString();

        try {
            // Capture the state before rule execution for auditing
            Map<String, Object> beforeState = new HashMap<>(dynamicObject.getProperties());

            // Add rule firing listener for audit logging
            kieSession.addEventListener(new DefaultAgendaEventListener() {
                @Override
                public void afterMatchFired(AfterMatchFiredEvent event) {
                    String ruleName = event.getMatch().getRule().getName();
                    auditService.recordRuleExecution(ruleName, objectId, beforeState);
                }
            });

            // Execute rules
            kieSession.insert(dynamicObject);
            kieSession.fireAllRules();

            // Record the final state after all rules have executed
            auditService.completeRuleExecution(objectId, dynamicObject.getProperties());

            return dynamicObject;
        } finally {
            kieSession.dispose();
        }
    }
}