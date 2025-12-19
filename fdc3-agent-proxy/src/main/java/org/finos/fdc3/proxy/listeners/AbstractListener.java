/**
 * Copyright 2023 Wellington Management Company LLP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.finos.fdc3.proxy.listeners;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.types.Listener;
import org.finos.fdc3.proxy.Messaging;

/**
 * Common base for all listeners - handles registration and unregistration with messaging
 * and sends notification messages when connected and disconnected.
 * 
 * This mirrors the TypeScript AbstractListener pattern.
 *
 * @param <H> The handler type (e.g., ContextHandler, IntentHandler, EventHandler)
 */
public abstract class AbstractListener<H> implements RegisterableListener, Listener {

    protected final Messaging messaging;
    protected final long messageExchangeTimeout;
    protected final H handler;
    
    private final String subscribeRequestType;
    private final String subscribeResponseType;
    private final String unsubscribeRequestType;
    private final String unsubscribeResponseType;
    
    // The listener ID assigned by the server
    protected String id = null;

    /**
     * Construct an AbstractListener.
     *
     * @param messaging the messaging system
     * @param messageExchangeTimeout timeout for message exchanges
     * @param handler the handler callback
     * @param subscribeRequestType the type string for subscribe requests (e.g., "addContextListenerRequest")
     * @param subscribeResponseType the type string for subscribe responses (e.g., "addContextListenerResponse")
     * @param unsubscribeRequestType the type string for unsubscribe requests (e.g., "contextListenerUnsubscribeRequest")
     * @param unsubscribeResponseType the type string for unsubscribe responses (e.g., "contextListenerUnsubscribeResponse")
     */
    protected AbstractListener(
            Messaging messaging,
            long messageExchangeTimeout,
            H handler,
            String subscribeRequestType,
            String subscribeResponseType,
            String unsubscribeRequestType,
            String unsubscribeResponseType) {
        this.messaging = messaging;
        this.messageExchangeTimeout = messageExchangeTimeout;
        this.handler = handler;
        this.subscribeRequestType = subscribeRequestType;
        this.subscribeResponseType = subscribeResponseType;
        this.unsubscribeRequestType = unsubscribeRequestType;
        this.unsubscribeResponseType = unsubscribeResponseType;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Filter function to determine if a message should be processed.
     * Subclasses implement this to define their filtering logic.
     *
     * @param message the incoming message
     * @return true if the message should be processed
     */
    @Override
    public abstract boolean filter(Map<String, Object> message);

    /**
     * Action to perform when a matching message is received.
     * Subclasses implement this to define their handling logic.
     *
     * @param message the matched message
     */
    @Override
    public abstract void action(Map<String, Object> message);

    /**
     * Build the subscription request payload.
     * Subclasses implement this to provide their specific payload.
     *
     * @return the subscription request as a Map
     */
    protected abstract Map<String, Object> buildSubscribeRequest();

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> register() {
        Map<String, Object> request = buildSubscribeRequest();
        request.put("type", subscribeRequestType);
        request.put("meta", messaging.getConverter().toMap(messaging.createMeta()));

        return messaging.<Map<String, Object>>exchange(request, subscribeResponseType, messageExchangeTimeout)
                .thenAccept(response -> {
                    // Extract listenerUUID from the response
                    Map<String, Object> payload = (Map<String, Object>) response.get("payload");
                    if (payload != null) {
                        this.id = (String) payload.get("listenerUUID");
                    }
                    
                    if (this.id == null) {
                        throw new RuntimeException(
                            "The Desktop Agent's response did not include a listenerUUID, " +
                            "which means this listener can't be removed!");
                    }
                    
                    messaging.register(this);
                });
    }

    @Override
    public CompletionStage<Void> unsubscribe() {
        if (this.id != null) {
            messaging.unregister(this.id);
            
            Map<String, Object> request = new java.util.HashMap<>();
            request.put("type", unsubscribeRequestType);
            request.put("meta", messaging.getConverter().toMap(messaging.createMeta()));
            
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("listenerUUID", this.id);
            request.put("payload", payload);
            
            return messaging.<Map<String, Object>>exchange(request, unsubscribeResponseType, messageExchangeTimeout)
                    .thenAccept(response -> { /* completed */ });
        } else {
            throw new RuntimeException("This listener doesn't have an id and hence can't be removed!");
        }
    }
}

