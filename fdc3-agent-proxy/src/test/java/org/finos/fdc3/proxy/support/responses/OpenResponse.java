/**
 * Copyright FINOS and its Contributors
 */
package org.finos.fdc3.proxy.support.responses;

import static org.finos.fdc3.proxy.support.responses.ResponseSupport.createResponseMeta;
import static org.finos.fdc3.proxy.support.responses.ResponseSupport.scheduleReceive;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.proxy.support.TestMessaging;
import org.finos.fdc3.proxy.support.TestMessaging.IntentDetail;

/**
 * Responds to open requests.
 */
public class OpenResponse implements AutomaticResponse {
    
    @Override
    public boolean filter(String messageType) {
        return "openRequest".equals(messageType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> action(Map<String, Object> message, TestMessaging messaging) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        Map<String, Object> msgPayload = (Map<String, Object>) message.get("payload");
        Map<String, Object> app = (Map<String, Object>) msgPayload.get("app");
        
        String appId = app != null ? (String) app.get("appId") : null;
        
        Map<String, Object> payload = new HashMap<>();
        
        // Find the app in intent details
        IntentDetail found = null;
        for (IntentDetail detail : messaging.getIntentDetails()) {
            if (detail.getApp() != null && appId != null && 
                appId.equals(detail.getApp().getAppId())) {
                found = detail;
                break;
            }
        }
        
        if (found != null && found.getApp() != null) {
            Map<String, Object> appIdentifier = new HashMap<>();
            appIdentifier.put("appId", found.getApp().getAppId());
            appIdentifier.put("instanceId", "abc123");
            payload.put("appIdentifier", appIdentifier);
        } else {
            payload.put("error", "AppNotFound");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "openResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", payload);
        
        scheduleReceive(messaging, response);
        return CompletableFuture.completedFuture(null);
    }
}

