/**
 * Copyright 2023 Wellington Management Company LLP
 */
package org.finos.fdc3.proxy.support.responses;

import static org.finos.fdc3.proxy.support.responses.ResponseSupport.createResponseMeta;
import static org.finos.fdc3.proxy.support.responses.ResponseSupport.scheduleReceive;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.proxy.support.TestMessaging;

/**
 * Responds to getAppMetadata requests.
 */
public class GetAppMetadataResponse implements AutomaticResponse {
    
    @Override
    public boolean filter(String messageType) {
        return "getAppMetadataRequest".equals(messageType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> action(Map<String, Object> message, TestMessaging messaging) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        Map<String, Object> msgPayload = (Map<String, Object>) message.get("payload");
        Map<String, Object> app = (Map<String, Object>) msgPayload.get("app");
        
        String appId = app != null ? (String) app.get("appId") : "unknown";
        
        Map<String, Object> appMetadata = new HashMap<>();
        appMetadata.put("appId", appId);
        appMetadata.put("name", "Metadata Name");
        appMetadata.put("description", "Metadata Description");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("appMetadata", appMetadata);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "getAppMetadataResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", payload);
        
        scheduleReceive(messaging, response);
        return CompletableFuture.completedFuture(null);
    }
}

