/**
 * Copyright 2023 Wellington Management Company LLP
 */
package org.finos.fdc3.proxy.support.responses;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.proxy.support.TestMessaging;

import static org.finos.fdc3.proxy.support.responses.ResponseSupport.*;

/**
 * Responds to listener registration requests.
 */
public class RegisterListenersResponse implements AutomaticResponse {
    
    @Override
    public boolean filter(String messageType) {
        return "addContextListenerRequest".equals(messageType) ||
               "addIntentListenerRequest".equals(messageType) ||
               "addEventListenerRequest".equals(messageType) ||
               "privateChannelAddEventListenerRequest".equals(messageType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> action(Map<String, Object> message, TestMessaging messaging) {
        String type = (String) message.get("type");
        String responseType = type.replace("Request", "Response");
        
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("listenerUUID", UUID.randomUUID().toString());
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", responseType);
        response.put("meta", createResponseMeta(meta));
        response.put("payload", payload);
        
        scheduleReceive(messaging, response);
        return CompletableFuture.completedFuture(null);
    }
}

