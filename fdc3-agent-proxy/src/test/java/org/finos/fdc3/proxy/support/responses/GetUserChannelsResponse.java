/**
 * Copyright 2023 Wellington Management Company LLP
 */
package org.finos.fdc3.proxy.support.responses;

import static org.finos.fdc3.proxy.support.responses.ResponseSupport.createResponseMeta;
import static org.finos.fdc3.proxy.support.responses.ResponseSupport.scheduleReceive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.proxy.support.TestMessaging;

/**
 * Responds to getUserChannels requests.
 */
public class GetUserChannelsResponse implements AutomaticResponse {
    
    @Override
    public boolean filter(String messageType) {
        return "getUserChannelsRequest".equals(messageType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> action(Map<String, Object> message, TestMessaging messaging) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        
        List<Map<String, Object>> userChannels = new ArrayList<>();
        for (String channelId : messaging.getChannelState().keySet()) {
            Map<String, Object> channel = new HashMap<>();
            channel.put("id", channelId);
            channel.put("type", "user");
            
            Map<String, String> displayMetadata = new HashMap<>();
            displayMetadata.put("name", "The " + channelId + " channel");
            displayMetadata.put("color", "red");
            displayMetadata.put("glyph", "triangle");
            channel.put("displayMetadata", displayMetadata);
            
            userChannels.add(channel);
        }
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("userChannels", userChannels);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "getUserChannelsResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", payload);
        
        scheduleReceive(messaging, response);
        return CompletableFuture.completedFuture(null);
    }
}

