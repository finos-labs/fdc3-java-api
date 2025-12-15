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
 * Responds to createPrivateChannel requests.
 */
public class CreatePrivateChannelResponse implements AutomaticResponse {
    
    @Override
    public boolean filter(String messageType) {
        return "createPrivateChannelRequest".equals(messageType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> action(Map<String, Object> message, TestMessaging messaging) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        
        String channelId = "private-" + UUID.randomUUID().toString();
        
        Map<String, Object> channel = new HashMap<>();
        channel.put("id", channelId);
        channel.put("type", "private");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("privateChannel", channel);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "createPrivateChannelResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", payload);
        
        scheduleReceive(messaging, response);
        return CompletableFuture.completedFuture(null);
    }
}

