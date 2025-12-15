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

import java.util.HashMap;
import java.util.Map;

import org.finos.fdc3.api.types.EventHandler;
import org.finos.fdc3.api.types.FDC3Event;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.schema.PrivateChannelEventType;

/**
 * Event listener for private channel events.
 * Extends AbstractListener to handle registration/unregistration.
 */
public class PrivateChannelEventListener extends AbstractListener<EventHandler> {

    private final String channelId;
    private final String eventType;

    public PrivateChannelEventListener(
            Messaging messaging,
            long messageExchangeTimeout,
            String channelId,
            String eventType,
            EventHandler handler) {
        super(
            messaging,
            messageExchangeTimeout,
            handler,
            "privateChannelAddEventListenerRequest",
            "privateChannelAddEventListenerResponse",
            "privateChannelUnsubscribeEventListenerRequest",
            "privateChannelUnsubscribeEventListenerResponse"
        );
        this.channelId = channelId;
        this.eventType = eventType;
    }

    @Override
    protected Map<String, Object> buildSubscribeRequest() {
        Map<String, Object> request = new HashMap<>();
        Map<String, Object> payload = new HashMap<>();
        payload.put("privateChannelId", channelId);
        PrivateChannelEventType pcEventType = toPrivateChannelEventType(eventType);
        if (pcEventType != null) {
            payload.put("listenerType", pcEventType.toValue());
        }
        request.put("payload", payload);
        return request;
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

    private PrivateChannelEventType toPrivateChannelEventType(String eventType) {
        if (eventType == null) {
            return null;
        }
        switch (eventType) {
            case "addContextListener":
                return PrivateChannelEventType.ADD_CONTEXT_LISTENER;
            case "unsubscribe":
                return PrivateChannelEventType.UNSUBSCRIBE;
            case "disconnect":
                return PrivateChannelEventType.DISCONNECT;
            default:
                return null;
        }
    }
}
