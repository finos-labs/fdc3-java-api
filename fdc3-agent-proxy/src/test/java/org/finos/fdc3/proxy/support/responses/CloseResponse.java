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

import org.finos.fdc3.api.errors.CloseError;
import org.finos.fdc3.proxy.support.TestMessaging;

/**
 * Responds to close requests.
 */
public class CloseResponse implements AutomaticResponse {

    @Override
    public boolean filter(String messageType) {
        return "closeRequest".equals(messageType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> action(Map<String, Object> message, TestMessaging messaging) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        Map<String, Object> payload = new HashMap<>();

        if (messaging.isCloseShouldFail()) {
            payload.put("error", CloseError.ErrorOnClose.toString());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", "closeResponse");
        response.put("meta", createResponseMeta(meta));
        response.put("payload", payload);

        scheduleReceive(messaging, response);
        return CompletableFuture.completedFuture(null);
    }
}
