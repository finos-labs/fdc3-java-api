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
import java.util.concurrent.atomic.AtomicInteger;

import org.finos.fdc3.proxy.support.TestMessaging;

/**
 * Responds to addEventListener requests.
 */
public class AddEventListenerResponse implements AutomaticResponse {
    
    private final AtomicInteger count = new AtomicInteger(0);
    
    @Override
    public boolean filter(String messageType) {
        return "addEventListenerRequest".equals(messageType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> action(Map<String, Object> message, TestMessaging messaging) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("listenerUUID", "listener-" + count.getAndIncrement());
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "addEventListenerResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", payload);
        
        scheduleReceive(messaging, response);
        return CompletableFuture.completedFuture(null);
    }
}

