/**
 * Copyright 2023 Wellington Management Company LLP
 */
package org.finos.fdc3.proxy.support.responses;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.proxy.support.TestMessaging;

/**
 * Interface for automatic responses to test messages.
 */
public interface AutomaticResponse {
    
    /**
     * Returns true if this response should handle messages of the given type.
     */
    boolean filter(String messageType);
    
    /**
     * Processes the message and sends an appropriate response.
     */
    CompletionStage<Void> action(Map<String, Object> message, TestMessaging messaging);
}

