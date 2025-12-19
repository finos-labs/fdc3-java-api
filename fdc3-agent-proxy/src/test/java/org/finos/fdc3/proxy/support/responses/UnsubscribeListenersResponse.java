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
 * Responds to listener unsubscribe requests.
 */
public class UnsubscribeListenersResponse implements AutomaticResponse {
    
    @Override
    public boolean filter(String messageType) {
        return "contextListenerUnsubscribeRequest".equals(messageType) ||
               "intentListenerUnsubscribeRequest".equals(messageType) ||
               "eventListenerUnsubscribeRequest".equals(messageType) ||
               "privateChannelUnsubscribeEventListenerRequest".equals(messageType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> action(Map<String, Object> message, TestMessaging messaging) {
        String type = (String) message.get("type");
        String responseType = type.replace("Request", "Response");
        
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", responseType);
        response.put("meta", createResponseMeta(meta));
        response.put("payload", new HashMap<>());
        
        scheduleReceive(messaging, response);
        return CompletableFuture.completedFuture(null);
    }
}

