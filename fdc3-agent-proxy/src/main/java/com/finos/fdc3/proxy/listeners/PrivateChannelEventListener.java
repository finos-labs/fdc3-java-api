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

package com.finos.fdc3.proxy.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.finos.fdc3.api.types.EventHandler;
import com.finos.fdc3.api.types.FDC3Event;
import com.finos.fdc3.api.types.Listener;
import com.finos.fdc3.proxy.Messaging;

/**
 * Event listener for private channel events.
 */
public class PrivateChannelEventListener implements RegisterableListener, Listener {

    private final Messaging messaging;
    private final long messageExchangeTimeout;
    private final String channelId;
    private final String eventType;
    private final EventHandler handler;
    private final String id;

    public PrivateChannelEventListener(
            Messaging messaging,
            long messageExchangeTimeout,
            String channelId,
            String eventType,
            EventHandler handler) {
        this.messaging = messaging;
        this.messageExchangeTimeout = messageExchangeTimeout;
        this.channelId = channelId;
        this.eventType = eventType;
        this.handler = handler;
        this.id = messaging.createUUID();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean filter(Map<String, Object> message) {
        String type = (String) message.get("type");
        if (!getExpectedMessageType().equals(type)) {
            return false;
        }

        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        if (payload == null) {
            return false;
        }

        String msgChannelId = (String) payload.get("privateChannelId");
        return channelId.equals(msgChannelId);
    }

    private String getExpectedMessageType() {
        if (eventType == null) {
            return "privateChannelOnEvent";
        }
        switch (eventType) {
            case "addContextListener":
                return "privateChannelOnAddContextListenerEvent";
            case "unsubscribe":
                return "privateChannelOnUnsubscribeEvent";
            case "disconnect":
                return "privateChannelOnDisconnectEvent";
            default:
                return "privateChannelOnEvent";
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void action(Map<String, Object> message) {
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        FDC3Event<Map<String, Object>> event = new FDC3Event<>(eventType, payload);
        handler.handleEvent(event);
    }

    @Override
    public CompletionStage<Void> register() {
        Map<String, Object> request = new HashMap<>();
        request.put("meta", messaging.createMeta());
        request.put("type", "privateChannelAddEventListenerRequest");

        Map<String, Object> payload = new HashMap<>();
        payload.put("privateChannelId", channelId);
        payload.put("listenerType", eventType);
        request.put("payload", payload);

        messaging.register(this);

        return messaging.<Map<String, Object>>exchange(request, "privateChannelAddEventListenerResponse", messageExchangeTimeout)
                .thenApply(response -> null);
    }

    @Override
    public void unsubscribe() {
        messaging.unregister(id);
    }

    /**
     * Register synchronously.
     */
    public void registerSync() {
        try {
            register().toCompletableFuture().get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to register listener", e);
        }
    }
}

