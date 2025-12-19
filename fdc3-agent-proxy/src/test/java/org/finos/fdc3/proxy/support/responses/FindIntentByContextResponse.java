/**
 * Copyright 2023 Wellington Management Company LLP
 */
package org.finos.fdc3.proxy.support.responses;

import static org.finos.fdc3.proxy.support.responses.ResponseSupport.createResponseMeta;
import static org.finos.fdc3.proxy.support.responses.ResponseSupport.scheduleReceive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.proxy.support.TestMessaging;
import org.finos.fdc3.proxy.support.TestMessaging.IntentDetail;

/**
 * Responds to findIntentsByContext requests.
 */
public class FindIntentByContextResponse implements AutomaticResponse {
    
    @Override
    public boolean filter(String messageType) {
        return "findIntentsByContextRequest".equals(messageType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> action(Map<String, Object> message, TestMessaging messaging) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        Map<String, Object> msgPayload = (Map<String, Object>) message.get("payload");
        
        Map<String, Object> context = (Map<String, Object>) msgPayload.get("context");
        String contextType = context != null ? (String) context.get("type") : null;
        
        // Find matching intent details by context type
        List<IntentDetail> matching = new ArrayList<>();
        for (IntentDetail detail : messaging.getIntentDetails()) {
            if (contextType == null || contextType.equals(detail.getContext())) {
                matching.add(detail);
            }
        }
        
        // Get unique intent names
        Set<String> uniqueIntents = new LinkedHashSet<>();
        for (IntentDetail detail : matching) {
            if (detail.getIntent() != null) {
                uniqueIntents.add(detail.getIntent());
            }
        }
        
        // Build appIntents
        List<Map<String, Object>> appIntents = new ArrayList<>();
        for (String intentName : uniqueIntents) {
            Map<String, Object> intentInfo = new LinkedHashMap<>();
            intentInfo.put("name", intentName);
            intentInfo.put("displayName", intentName);
            
            List<Map<String, String>> apps = new ArrayList<>();
            for (IntentDetail detail : matching) {
                if (intentName.equals(detail.getIntent()) && detail.getApp() != null) {
                    Map<String, String> app = new LinkedHashMap<>();
                    app.put("appId", detail.getApp().getAppId());
                    if (detail.getApp().getInstanceId() != null) { app.put("instanceId", detail.getApp().getInstanceId()); }
                    apps.add(app);
                }
            }
            
            Map<String, Object> appIntent = new HashMap<>();
            appIntent.put("intent", intentInfo);
            appIntent.put("apps", apps);
            appIntents.add(appIntent);
        }
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("appIntents", appIntents);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "findIntentsByContextResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", payload);
        
        scheduleReceive(messaging, response);
        return CompletableFuture.completedFuture(null);
    }
}

