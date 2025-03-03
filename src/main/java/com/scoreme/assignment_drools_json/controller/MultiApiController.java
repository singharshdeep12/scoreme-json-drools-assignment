package com.scoreme.assignment_drools_json.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.scoreme.assignment_drools_json.model.DynamicObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.scoreme.assignment_drools_json.service.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/process")
public class MultiApiController {

    @Autowired
    private ApiResponseHandler apiResponseHandler;

    @Autowired
    private DynamicJsonService jsonService;

    @Autowired
    private DroolsService droolsService;


    /**
     * Maintain the original endpoint for backward compatibility
     */
    @PostMapping("/evaluate")
    public ResponseEntity<String> processJson(@RequestBody String jsonRequest) {
        try {
            // Create dynamic object
            DynamicObject dynamicObject = jsonService.convertJsonToDynamicObject(jsonRequest);

            // Apply rules
            droolsService.processRules(dynamicObject);

            // Apply changes back to original structure
            dynamicObject.applyChangesToOriginal();

            // Return modified JSON
            return ResponseEntity.ok(new Gson().toJson(dynamicObject.getOriginalJson()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Process a single API response
     */
    @PostMapping("/{apiType}")
    public ResponseEntity<String> processSingleApi(
            @PathVariable String apiType,
            @RequestBody String jsonRequest) {

        try {
            String processedResponse = apiResponseHandler.processApiResponse(jsonRequest, apiType);
            return ResponseEntity.ok(processedResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Process multiple API responses together
     */
    @PostMapping("/process-multiple")
    public ResponseEntity<String> processMultipleApis(@RequestBody String jsonRequest) {
        try {
            // Parse the incoming request which should be a map of API type to response
            JsonObject requestObj = new Gson().fromJson(jsonRequest, JsonObject.class);
            Map<String, String> apiResponses = new HashMap<>();

            // Extract each API response
            for (String apiType : requestObj.keySet()) {
                apiResponses.put(apiType, requestObj.get(apiType).toString());
            }

            // Process all responses
            Map<String, String> processedResponses =
                    apiResponseHandler.processMultipleResponses(apiResponses);

            // Convert back to JSON
            return ResponseEntity.ok(new Gson().toJson(processedResponses));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }


}