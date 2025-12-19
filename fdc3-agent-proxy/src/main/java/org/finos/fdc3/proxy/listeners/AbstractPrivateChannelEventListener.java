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
import java.util.List;
import java.util.Map;

import org.finos.fdc3.api.types.EventHandler;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.schema.PrivateChannelEventType;

/**
 * Abstract base class for private channel event listeners.
 * Extends AbstractListener to handle registration/unregistration.
 */
public abstract class AbstractPrivateChannelEventListener extends AbstractListener<EventHandler> {

    protected final String privateChannelId;
    protected final List<String> eventMessageTypes;
    protected final String eventType;

    protected AbstractPrivateChannelEventListener(
            Messaging messaging,
            long messageExchangeTimeout,
            String privateChannelId,
            List<String> eventMessageTypes,
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
        this.privateChannelId = privateChannelId;
        this.eventMessageTypes = eventMessageTypes;
        this.eventType = eventType;
    }

    @Override
    protected Map<String, Object> buildSubscribeRequest() {
        Map<String, Object> request = new HashMap<>();
        Map<String, Object> payload = new HashMap<>();
        payload.put("privateChannelId", privateChannelId);
        PrivateChannelEventType pcEventType = toPrivateChannelEventType(eventType);
        // Explicitly set listenerType to null if eventType is null, otherwise use the enum value
        payload.put("listenerType", pcEventType != null ? pcEventType.toValue() : null);
        request.put("payload", payload);
        return request;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean filter(Map<String, Object> message) {
        String type = (String) message.get("type");
        if (!eventMessageTypes.contains(type)) {
            return false;
        }

        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        if (payload == null) {
            return false;
        }

        String msgChannelId = (String) payload.get("privateChannelId");
        return privateChannelId.equals(msgChannelId);
    }

    @Override
    public abstract void action(Map<String, Object> message);

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
                throw new RuntimeException("Unsupported event type: " + eventType);
        }
    }
}

