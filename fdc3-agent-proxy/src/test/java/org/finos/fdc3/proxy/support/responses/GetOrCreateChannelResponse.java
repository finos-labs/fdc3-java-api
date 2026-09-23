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
import java.util.concurrent.ConcurrentHashMap;

import org.finos.fdc3.proxy.support.TestMessaging;

/**
 * Responds to getOrCreateChannel requests.
 */
public class GetOrCreateChannelResponse implements AutomaticResponse {
    
    private final Map<String, String> channelTypes = new ConcurrentHashMap<>();
    
    @Override
    public boolean filter(String messageType) {
        return "getOrCreateChannelRequest".equals(messageType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> action(Map<String, Object> message, TestMessaging messaging) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        Map<String, Object> msgPayload = (Map<String, Object>) message.get("payload");
        String channelId = (String) msgPayload.get("channelId");
        String type = "app";
        
        String existingType = channelTypes.get(channelId);
        
        Map<String, Object> payload = new HashMap<>();
        
        if (existingType != null && !existingType.equals(type)) {
            // channel already exists with different type
            payload.put("error", "AccessDenied");
        } else {
            channelTypes.put(channelId, type);
            
            Map<String, Object> channel = new HashMap<>();
            channel.put("id", channelId);
            channel.put("type", type);
            
            Map<String, String> displayMetadata = new HashMap<>();
            displayMetadata.put("name", "The " + channelId + " Channel");
            displayMetadata.put("color", "cerulean blue");
            displayMetadata.put("glyph", "triangle");
            channel.put("displayMetadata", displayMetadata);
            
            payload.put("channel", channel);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "getOrCreateChannelResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", payload);
        
        scheduleReceive(messaging, response);
        return CompletableFuture.completedFuture(null);
    }
}

