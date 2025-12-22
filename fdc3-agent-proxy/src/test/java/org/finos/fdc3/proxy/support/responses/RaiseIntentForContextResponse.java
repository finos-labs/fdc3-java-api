/**
 * Copyright 2023 Wellington Management Company LLP
 */
package org.finos.fdc3.proxy.support.responses;

import static org.finos.fdc3.proxy.support.responses.ResponseSupport.createResponseMeta;
import static org.finos.fdc3.proxy.support.responses.ResponseSupport.scheduleReceive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.finos.fdc3.proxy.support.TestMessaging;
import org.finos.fdc3.proxy.support.TestMessaging.IntentDetail;
import org.finos.fdc3.proxy.support.TestMessaging.PossibleIntentResult;

/**
 * Responds to raiseIntentForContext requests.
 */
public class RaiseIntentForContextResponse implements AutomaticResponse {
    
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    @Override
    public boolean filter(String messageType) {
        return "raiseIntentForContextRequest".equals(messageType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> action(Map<String, Object> message, TestMessaging messaging) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        Map<String, Object> msgPayload = (Map<String, Object>) message.get("payload");
        
        Map<String, Object> context = (Map<String, Object>) msgPayload.get("context");
        String contextType = context != null ? (String) context.get("type") : null;
        Map<String, Object> targetApp = (Map<String, Object>) msgPayload.get("app");
        
        PossibleIntentResult intentResult = messaging.getIntentResult();
        
        if (intentResult == null) {
            // Figure out response based on app details
            List<IntentDetail> matching = findMatchingIntents(messaging, contextType, targetApp);
            Map<String, Object> response = createRaiseIntentForContextResponse(meta, matching, messaging);
            scheduleReceive(messaging, response);
        } else if (!intentResult.isTimeout()) {
            // Send pre-set intent resolution
            Map<String, Object> response = createCannedResponse(meta, messaging);
            scheduleReceive(messaging, response);
            
            // Then send the result response
            if (intentResult.getError() == null) {
                Map<String, Object> resultResponse = createRaiseIntentResultResponse(meta, messaging);
                if (resultResponse != null) {
                    scheduler.schedule(() -> {
                        messaging.receive(resultResponse, null);
                    }, 300, TimeUnit.MILLISECONDS);
                }
            }
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    private List<IntentDetail> findMatchingIntents(TestMessaging messaging, String contextType, 
                                                    Map<String, Object> targetApp) {
        List<IntentDetail> matching = new ArrayList<>();
        for (IntentDetail detail : messaging.getIntentDetails()) {
            boolean matches = true;
            
            // Match context type - null is a wildcard that matches anything
            // If either contextType (from request) or detail.getContext() is null, it matches
            if (contextType != null && detail.getContext() != null && !contextType.equals(detail.getContext())) {
                matches = false;
            }
            
            // Match target app if specified
            if (matches && targetApp != null && detail.getApp() != null) {
                String targetAppId = (String) targetApp.get("appId");
                String targetInstanceId = (String) targetApp.get("instanceId");
                if (targetAppId != null && !targetAppId.equals(detail.getApp().getAppId())) {
                    matches = false;
                }
                if (matches && targetInstanceId != null && 
                    detail.getApp().getInstanceId() != null && 
                    !targetInstanceId.equals(detail.getApp().getInstanceId())) {
                    matches = false;
                }
            }
            
            if (matches) {
                matching.add(detail);
            }
        }
        return matching;
    }
    
    private Map<String, Object> createRaiseIntentForContextResponse(Map<String, Object> meta, 
                                                                     List<IntentDetail> relevant, 
                                                                     TestMessaging messaging) {
        Map<String, Object> payload = new HashMap<>();
        
        if (relevant.isEmpty()) {
            payload.put("error", "NoAppsFound");
        } else if (relevant.size() == 1 && relevant.get(0).getIntent() != null && relevant.get(0).getApp() != null) {
            IntentDetail detail = relevant.get(0);
            Map<String, Object> source = new HashMap<>();
            source.put("appId", detail.getApp().getAppId());
            if (detail.getApp().getInstanceId() != null) { source.put("instanceId", detail.getApp().getInstanceId()); }
            
            Map<String, Object> resolution = new HashMap<>();
            resolution.put("intent", detail.getIntent());
            resolution.put("source", source);
            payload.put("intentResolution", resolution);
        } else {
            // Multiple intents found - return appIntents for disambiguation
            Set<String> uniqueIntents = new LinkedHashSet<>();
            for (IntentDetail detail : relevant) {
                if (detail.getIntent() != null) {
                    uniqueIntents.add(detail.getIntent());
                }
            }
            
            List<Map<String, Object>> appIntents = new ArrayList<>();
            for (String intentName : uniqueIntents) {
                Map<String, Object> intentInfo = new LinkedHashMap<>();
                intentInfo.put("name", intentName);
                intentInfo.put("displayName", intentName);
                
                List<Map<String, String>> apps = new ArrayList<>();
                for (IntentDetail detail : relevant) {
                    if (intentName.equals(detail.getIntent()) && detail.getApp() != null) {
                        Map<String, String> app = new LinkedHashMap<>();
                        app.put("appId", detail.getApp().getAppId());
                        if (detail.getApp().getInstanceId() != null) { app.put("instanceId", detail.getApp().getInstanceId()); }
                        apps.add(app);
                    }
                }
                
                Map<String, Object> appIntent = new LinkedHashMap<>();
                appIntent.put("intent", intentInfo);
                appIntent.put("apps", apps);
                appIntents.add(appIntent);
            }
            
            payload.put("appIntents", appIntents);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "raiseIntentForContextResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", payload);
        
        return response;
    }
    
    private Map<String, Object> createCannedResponse(Map<String, Object> meta, TestMessaging messaging) {
        PossibleIntentResult result = messaging.getIntentResult();
        
        Map<String, Object> payload = new HashMap<>();
        
        if (result != null && result.getError() != null) {
            payload.put("error", result.getError());
        } else {
            Map<String, Object> source = new HashMap<>();
            source.put("appId", "some-app");
            source.put("instanceId", "abc123");
            
            Map<String, Object> resolution = new HashMap<>();
            resolution.put("intent", "some-canned-intent");
            resolution.put("source", source);
            payload.put("intentResolution", resolution);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "raiseIntentForContextResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", payload);
        
        return response;
    }
    
    private Map<String, Object> createRaiseIntentResultResponse(Map<String, Object> meta, TestMessaging messaging) {
        PossibleIntentResult result = messaging.getIntentResult();
        if (result == null || result.getError() != null) {
            return null;
        }
        
        Map<String, Object> intentResult = new HashMap<>();
        if (result.getContext() != null) {
            intentResult.put("context", result.getContext());
        }
        if (result.getChannel() != null) {
            Map<String, Object> channelMap = new HashMap<>();
            channelMap.put("id", result.getChannel().getId());
            channelMap.put("type", result.getChannel().getType());
            intentResult.put("channel", channelMap);
        }
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("intentResult", intentResult);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "raiseIntentResultResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", payload);
        
        return response;
    }
}

