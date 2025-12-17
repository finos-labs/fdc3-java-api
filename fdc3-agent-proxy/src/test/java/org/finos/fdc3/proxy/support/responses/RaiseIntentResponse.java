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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.finos.fdc3.proxy.support.TestMessaging;
import org.finos.fdc3.proxy.support.TestMessaging.IntentDetail;
import org.finos.fdc3.proxy.support.TestMessaging.PossibleIntentResult;

import static org.finos.fdc3.proxy.support.responses.ResponseSupport.*;

/**
 * Responds to raiseIntent requests.
 */
public class RaiseIntentResponse implements AutomaticResponse {
    
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    @Override
    public boolean filter(String messageType) {
        return "raiseIntentRequest".equals(messageType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> action(Map<String, Object> message, TestMessaging messaging) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        Map<String, Object> msgPayload = (Map<String, Object>) message.get("payload");
        
        String intent = (String) msgPayload.get("intent");
        Map<String, Object> context = (Map<String, Object>) msgPayload.get("context");
        String contextType = context != null ? (String) context.get("type") : null;
        Map<String, Object> targetApp = (Map<String, Object>) msgPayload.get("app");
        
        PossibleIntentResult intentResult = messaging.getIntentResult();
        
        if (intentResult == null) {
            // Figure out response based on app details (like FindIntent)
            List<IntentDetail> matching = findMatchingIntents(messaging, intent, contextType, targetApp);
            Map<String, Object> response = createRaiseIntentResponse(meta, message, matching, messaging);
            scheduleReceive(messaging, response);
        } else if (!intentResult.isTimeout()) {
            // Send pre-set intent resolution
            Map<String, Object> response = createCannedRaiseIntentResponse(meta, message, messaging);
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
    
    private List<IntentDetail> findMatchingIntents(TestMessaging messaging, String intent, 
                                                    String contextType, Map<String, Object> targetApp) {
        List<IntentDetail> matching = new ArrayList<>();
        for (IntentDetail detail : messaging.getIntentDetails()) {
            boolean matches = true;
            
            // Match intent name
            if (intent != null && detail.getIntent() != null && !intent.equals(detail.getIntent())) {
                matches = false;
            }
            
            // Match context type (optional)
            if (matches && contextType != null && detail.getContext() != null && !contextType.equals(detail.getContext())) {
                // Context type matching is optional
            }
            
            // Match target app if specified
            if (matches && targetApp != null && detail.getApp() != null) {
                String targetAppId = (String) targetApp.get("appId");
                String targetInstanceId = (String) targetApp.get("instanceId");
                if (targetAppId != null && !targetAppId.equals(detail.getApp().getAppID())) {
                    matches = false;
                }
                if (matches && targetInstanceId != null && 
                    detail.getApp().getInstanceID() != null && 
                    !targetInstanceId.equals(detail.getApp().getInstanceID())) {
                    matches = false;
                }
            }
            
            if (matches) {
                matching.add(detail);
            }
        }
        return matching;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> createRaiseIntentResponse(Map<String, Object> meta, Map<String, Object> message, 
                                                          List<IntentDetail> relevant, TestMessaging messaging) {
        Map<String, Object> msgPayload = (Map<String, Object>) message.get("payload");
        String intent = (String) msgPayload.get("intent");
        
        Map<String, Object> payload = new HashMap<>();
        
        if (relevant.isEmpty()) {
            payload.put("error", "NoAppsFound");
        } else if (relevant.size() == 1 && relevant.get(0).getIntent() != null && relevant.get(0).getApp() != null) {
            IntentDetail detail = relevant.get(0);
            Map<String, Object> source = new HashMap<>();
            source.put("appId", detail.getApp().getAppID());
            if (detail.getApp().getInstanceID() != null) { source.put("instanceId", detail.getApp().getInstanceID()); }
            
            Map<String, Object> resolution = new HashMap<>();
            resolution.put("intent", detail.getIntent());
            resolution.put("source", source);
            payload.put("intentResolution", resolution);
        } else {
            // Multiple apps found - return appIntent for disambiguation
            List<Map<String, String>> apps = new ArrayList<>();
            for (IntentDetail detail : relevant) {
                if (detail.getApp() != null && detail.getApp().getAppID() != null) {
                    Map<String, String> app = new HashMap<>();
                    app.put("appId", detail.getApp().getAppID());
                    if (detail.getApp().getInstanceID() != null) { app.put("instanceId", detail.getApp().getInstanceID()); }
                    apps.add(app);
                }
            }
            
            Map<String, Object> intentInfo = new HashMap<>();
            intentInfo.put("name", intent);
            intentInfo.put("displayName", intent);
            
            Map<String, Object> appIntent = new HashMap<>();
            appIntent.put("intent", intentInfo);
            appIntent.put("apps", apps);
            payload.put("appIntent", appIntent);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "raiseIntentResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", payload);
        
        return response;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> createCannedRaiseIntentResponse(Map<String, Object> meta, 
                                                                 Map<String, Object> message, 
                                                                 TestMessaging messaging) {
        PossibleIntentResult result = messaging.getIntentResult();
        Map<String, Object> msgPayload = (Map<String, Object>) message.get("payload");
        String intent = (String) msgPayload.get("intent");
        
        Map<String, Object> payload = new HashMap<>();
        
        if (result != null && result.getError() != null) {
            payload.put("error", result.getError());
        } else {
            Map<String, Object> source = new HashMap<>();
            source.put("appId", "some-app");
            source.put("instanceId", "abc123");
            
            Map<String, Object> resolution = new HashMap<>();
            resolution.put("intent", intent);
            resolution.put("source", source);
            payload.put("intentResolution", resolution);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "raiseIntentResponse");
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

