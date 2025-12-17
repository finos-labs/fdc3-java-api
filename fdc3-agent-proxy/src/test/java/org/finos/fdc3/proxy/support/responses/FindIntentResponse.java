/**
 * Copyright 2023 Wellington Management Company LLP
 */
package org.finos.fdc3.proxy.support.responses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.proxy.support.TestMessaging;
import org.finos.fdc3.proxy.support.TestMessaging.IntentDetail;

import static org.finos.fdc3.proxy.support.responses.ResponseSupport.*;

/**
 * Responds to findIntent requests.
 */
public class FindIntentResponse implements AutomaticResponse {
    
    @Override
    public boolean filter(String messageType) {
        return "findIntentRequest".equals(messageType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> action(Map<String, Object> message, TestMessaging messaging) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        Map<String, Object> msgPayload = (Map<String, Object>) message.get("payload");
        
        String intent = (String) msgPayload.get("intent");
        Map<String, Object> context = (Map<String, Object>) msgPayload.get("context");
        String contextType = context != null ? (String) context.get("type") : null;
        String resultType = (String) msgPayload.get("resultType");
        
        // Find matching intent details
        List<IntentDetail> matching = new ArrayList<>();
        for (IntentDetail detail : messaging.getIntentDetails()) {
            if (intentDetailMatches(detail, intent, contextType, resultType)) {
                matching.add(detail);
            }
        }
        
        // Build app list
        List<Map<String, String>> apps = new ArrayList<>();
        for (IntentDetail detail : matching) {
            if (detail.getApp() != null && detail.getApp().getAppID() != null) {
                Map<String, String> app = new HashMap<>();
                app.put("appId", detail.getApp().getAppID());
                if (detail.getApp().getInstanceID() != null) { app.put("instanceId", detail.getApp().getInstanceID()); }
                apps.add(app);
            }
        }
        
        Map<String, Object> intentInfo = new HashMap<>();
        intentInfo.put("name", intent);
        intentInfo.put("displayName", intent);
        
        Map<String, Object> appIntent = new HashMap<>();
        appIntent.put("intent", intentInfo);
        appIntent.put("apps", apps);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("appIntent", appIntent);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "findIntentResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", payload);
        
        scheduleReceive(messaging, response);
        return CompletableFuture.completedFuture(null);
    }
    
    private boolean intentDetailMatches(IntentDetail detail, String intent, String contextType, String resultType) {
        // Match intent
        if (intent != null && detail.getIntent() != null && !intent.equals(detail.getIntent())) {
            return false;
        }
        
        // Match context type (optional)
        if (contextType != null && detail.getContext() != null && !contextType.equals(detail.getContext())) {
            return false;
        }
        
        // Match result type (optional, with generic handling)
        if (resultType != null) {
            String detailResultType = detail.getResultType();
            if (detailResultType == null) {
                return false;
            }
            
            // Handle generic types
            if (resultType.contains("<")) {
                if (!resultType.equals(detailResultType)) {
                    return false;
                }
            } else {
                String actualType = removeGenericType(detailResultType);
                if (!resultType.equals(actualType)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private String removeGenericType(String type) {
        int start = type.indexOf('<');
        return start > 0 ? type.substring(0, start) : type;
    }
}

