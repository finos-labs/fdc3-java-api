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

import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.proxy.support.TestMessaging;
import org.finos.fdc3.proxy.support.TestMessaging.PossibleIntentResult;

/**
 * Responds to intentResult requests.
 */
public class IntentResultResponse implements AutomaticResponse {
    
    @Override
    public boolean filter(String messageType) {
        return "intentResultRequest".equals(messageType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> action(Map<String, Object> message, TestMessaging messaging) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        Map<String, Object> msgPayload = (Map<String, Object>) message.get("payload");
        
        // Store the intent result from the request
        Map<String, Object> intentResultPayload = (Map<String, Object>) msgPayload.get("intentResult");
        if (intentResultPayload != null) {
            PossibleIntentResult result = new PossibleIntentResult();
            
            Map<String, Object> contextMap = (Map<String, Object>) intentResultPayload.get("context");
            if (contextMap != null) {
                Context context = Context.fromMap(contextMap);
                result.setContext(context);
            }
            
            // Note: channel handling would require a Channel implementation
            // For now, we just store what we can
            
            messaging.setIntentResult(result);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "intentResultResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", new HashMap<>());
        
        scheduleReceive(messaging, response);
        return CompletableFuture.completedFuture(null);
    }
}

