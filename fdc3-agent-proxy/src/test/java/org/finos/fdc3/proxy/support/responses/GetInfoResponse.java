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

import org.finos.fdc3.proxy.support.TestMessaging;

/**
 * Responds to getInfo requests.
 */
public class GetInfoResponse implements AutomaticResponse {
    
    @Override
    public boolean filter(String messageType) {
        return "getInfoRequest".equals(messageType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> action(Map<String, Object> message, TestMessaging messaging) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        
        Map<String, Object> appMetadata = new HashMap<>();
        appMetadata.put("appId", "cucumber-app");
        appMetadata.put("instanceId", "cucumber-instance");
        
        Map<String, Object> optionalFeatures = new HashMap<>();
        optionalFeatures.put("DesktopAgentBridging", false);
        optionalFeatures.put("OriginatingAppMetadata", true);
        optionalFeatures.put("UserChannelMembershipAPIs", true);
        
        Map<String, Object> implementationMetadata = new HashMap<>();
        implementationMetadata.put("appMetadata", appMetadata);
        implementationMetadata.put("fdc3Version", "2.0");
        implementationMetadata.put("optionalFeatures", optionalFeatures);
        implementationMetadata.put("provider", "cucumber-provider");
        implementationMetadata.put("providerVersion", "test");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("implementationMetadata", implementationMetadata);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "getInfoResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", payload);
        
        scheduleReceive(messaging, response);
        return CompletableFuture.completedFuture(null);
    }
}

