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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.proxy.support.TestMessaging;

/**
 * Handles channel-related requests: broadcast, join, leave, getCurrentChannel,
 * addContextListener, contextListenerUnsubscribe, getCurrentContext.
 * 
 * Equivalent to ChannelState.ts in the TypeScript implementation.
 */
public class ChannelStateResponse implements AutomaticResponse {
    
    private String channelId = null;
    private final Map<String, List<String>> listeners = new HashMap<>();
    private final Map<String, List<Context>> contextHistory;
    
    public ChannelStateResponse(Map<String, List<Context>> contextHistory) {
        this.contextHistory = contextHistory != null ? contextHistory : new HashMap<>();
    }
    
    @Override
    public boolean filter(String messageType) {
        return "broadcastRequest".equals(messageType) ||
               "joinUserChannelRequest".equals(messageType) ||
               "leaveCurrentChannelRequest".equals(messageType) ||
               "getCurrentChannelRequest".equals(messageType) ||
               "addContextListenerRequest".equals(messageType) ||
               "contextListenerUnsubscribeRequest".equals(messageType) ||
               "getCurrentContextRequest".equals(messageType);
    }
    
    @Override
    public CompletionStage<Void> action(Map<String, Object> message, TestMessaging messaging) {
        String type = (String) message.get("type");
        Map<String, Object> response = null;
        
        switch (type) {
            case "broadcastRequest":
                response = createBroadcastResponse(message);
                break;
            case "joinUserChannelRequest":
                response = createJoinResponse(message);
                break;
            case "leaveCurrentChannelRequest":
                response = createLeaveResponse(message);
                break;
            case "getCurrentChannelRequest":
                response = createGetChannelResponse(message);
                break;
            case "addContextListenerRequest":
                response = createAddListenerResponse(message);
                break;
            case "contextListenerUnsubscribeRequest":
                response = createUnsubscribeResponse(message);
                break;
            case "getCurrentContextRequest":
                response = createGetContextResponse(message);
                break;
        }
        
        if (response != null) {
            scheduleReceive(messaging, response);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> createBroadcastResponse(Map<String, Object> message) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        String channel = (String) payload.get("channelId");
        Object context = payload.get("context");
        
        // Store context in history
        contextHistory.computeIfAbsent(channel, k -> new ArrayList<>());
        if (context instanceof Context) {
            contextHistory.get(channel).add(0, (Context) context);
        } else if (context instanceof Map) {
            contextHistory.get(channel).add(0, Context.fromMap((Map<String, Object>) context));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "broadcastResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", new HashMap<>());
        return response;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> createJoinResponse(Map<String, Object> message) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        Map<String, Object> msgPayload = (Map<String, Object>) message.get("payload");
        String requestedChannel = (String) msgPayload.get("channelId");
        
        Map<String, Object> responsePayload = new HashMap<>();
        
        if ("nonexistent".equals(requestedChannel)) {
            responsePayload.put("error", "NoChannelFound");
        } else {
            this.channelId = requestedChannel;
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "joinUserChannelResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", responsePayload);
        return response;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> createLeaveResponse(Map<String, Object> message) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        this.channelId = null;
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "leaveCurrentChannelResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", new HashMap<>());
        return response;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> createGetChannelResponse(Map<String, Object> message) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        
        Map<String, Object> responsePayload = new HashMap<>();
        if (this.channelId != null) {
            Map<String, Object> channel = new HashMap<>();
            channel.put("id", this.channelId);
            channel.put("type", "user");
            
            Map<String, Object> displayMetadata = new HashMap<>();
            displayMetadata.put("name", "The " + this.channelId + " channel");
            displayMetadata.put("color", "red");
            displayMetadata.put("glyph", "triangle");
            channel.put("displayMetadata", displayMetadata);
            
            responsePayload.put("channel", channel);
        } else {
            responsePayload.put("channel", null);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "getCurrentChannelResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", responsePayload);
        return response;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> createAddListenerResponse(Map<String, Object> message) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        String id = UUID.randomUUID().toString();
        
        if (this.channelId != null) {
            listeners.computeIfAbsent(this.channelId, k -> new ArrayList<>());
            listeners.get(this.channelId).add(id);
        }
        
        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("listenerUUID", id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "addContextListenerResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", responsePayload);
        return response;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> createUnsubscribeResponse(Map<String, Object> message) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        Map<String, Object> msgPayload = (Map<String, Object>) message.get("payload");
        String listenerId = (String) msgPayload.get("listenerUUID");
        
        // Remove listener from all channels
        for (List<String> channelListeners : listeners.values()) {
            channelListeners.remove(listenerId);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "contextListenerUnsubscribeResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", new HashMap<>());
        return response;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> createGetContextResponse(Map<String, Object> message) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        Map<String, Object> msgPayload = (Map<String, Object>) message.get("payload");
        String channel = (String) msgPayload.get("channelId");
        String contextType = (String) msgPayload.get("contextType");
        
        List<Context> contexts = contextHistory.get(channel);
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
                // Return first (most recent) context
                foundContext = contexts.get(0);
            }
        }
        
        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("context", foundContext);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "getCurrentContextResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", responsePayload);
        return response;
    }
}
