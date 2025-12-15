/**
 * Copyright 2023 Wellington Management Company LLP
 */
package org.finos.fdc3.proxy.support.responses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.proxy.support.TestMessaging;

import static org.finos.fdc3.proxy.support.responses.ResponseSupport.*;

/**
 * Responds to getCurrentContext requests.
 */
public class ChannelStateResponse implements AutomaticResponse {
    
    private final Map<String, List<Context>> channelState;
    
    public ChannelStateResponse(Map<String, List<Context>> channelState) {
        this.channelState = channelState;
    }
    
    @Override
    public boolean filter(String messageType) {
        return "getCurrentContextRequest".equals(messageType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> action(Map<String, Object> message, TestMessaging messaging) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        Map<String, Object> msgPayload = (Map<String, Object>) message.get("payload");
        String channelId = (String) msgPayload.get("channelId");
        String contextType = (String) msgPayload.get("contextType");
        
        List<Context> contexts = channelState.get(channelId);
        Context foundContext = null;
        
        if (contexts != null && !contexts.isEmpty()) {
            if (contextType != null) {
                // Find matching context type
                for (Context ctx : contexts) {
                    if (contextType.equals(ctx.getType())) {
                        foundContext = ctx;
                        break;
                    }
                }
            } else {
                // Return latest context
                foundContext = contexts.get(contexts.size() - 1);
            }
        }
        
        Map<String, Object> payload = new HashMap<>();
        if (foundContext != null) {
            payload.put("context", foundContext);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "getCurrentContextResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", payload);
        
        scheduleReceive(messaging, response);
        return CompletableFuture.completedFuture(null);
    }
}

