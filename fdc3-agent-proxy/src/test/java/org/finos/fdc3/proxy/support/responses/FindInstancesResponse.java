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

import static org.finos.fdc3.proxy.support.responses.ResponseSupport.*;

/**
 * Responds to findInstances requests.
 */
public class FindInstancesResponse implements AutomaticResponse {
    
    @Override
    public boolean filter(String messageType) {
        return "findInstancesRequest".equals(messageType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> action(Map<String, Object> message, TestMessaging messaging) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        
        List<Map<String, String>> appIdentifiers = new ArrayList<>();
        appIdentifiers.add(Map.of("appId", "One", "instanceId", "1"));
        appIdentifiers.add(Map.of("appId", "Two", "instanceId", "2"));
        appIdentifiers.add(Map.of("appId", "Three", "instanceId", "3"));
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("appIdentifiers", appIdentifiers);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "findInstancesResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", payload);
        
        scheduleReceive(messaging, response);
        return CompletableFuture.completedFuture(null);
    }
}

