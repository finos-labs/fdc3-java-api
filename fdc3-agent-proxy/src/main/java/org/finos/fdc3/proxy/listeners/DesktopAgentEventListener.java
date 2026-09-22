/**
 * Copyright FINOS and its Contributors
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

/**
 * Listener for Desktop Agent events.
 * Extends AbstractListener to handle registration/unregistration.
 */
public class DesktopAgentEventListener extends AbstractListener<EventHandler> {

    private final FDC3Event.Type eventType;

    public DesktopAgentEventListener(
            Messaging messaging,
            long messageExchangeTimeout,
            String eventType,
            EventHandler handler) {
        super(
            messaging,
            messageExchangeTimeout,
            handler,
            "addEventListenerRequest",
            "addEventListenerResponse",
            "eventListenerUnsubscribeRequest",
            "eventListenerUnsubscribeResponse"
        );
        this.eventType = parseDesktopAgentEventType(eventType);
    }

    /**
     * Parses and validates a Desktop Agent event type string.
     * {@code null} means listen to all events. Throws {@code RuntimeException}
     * with message {@code UnknownEventType} if not a Desktop Agent event type.
     */
    private static FDC3Event.Type parseDesktopAgentEventType(String eventType) {
        if (eventType == null) {
            return null;
        }
        final FDC3Event.Type type;
        try {
            type = FDC3Event.Type.fromValue(eventType);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("UnknownEventType");
        }
        switch (type) {
            case USER_CHANNEL_CHANGED:
            case CONTEXT_CLEARED:
                return type;
            default:
                throw new RuntimeException("UnknownEventType");
        }
    }

    @Override
    protected Map<String, Object> buildSubscribeRequest() {
        Map<String, Object> request = new HashMap<>();
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", eventType != null ? eventType.toWireValue() : null);
        // Desktop Agent-level listeners are registered with a null channelId
        payload.put("channelId", null);
        request.put("payload", payload);
        return request;
    }

    @Override
    public boolean filter(Map<String, Object> message) {
        String messageType = (String) message.get("type");
        if (messageType == null) {
            return false;
        }
        if (eventType == null) {
            // Wildcard listeners receive agent events only, not request/response traffic.
            return !messageType.endsWith("Response") && !messageType.endsWith("Request");
        }
        return eventType.toMessageType().equals(messageType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void action(Map<String, Object> message) {
        String messageType = (String) message.get("type");
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");

        FDC3Event.Type resolvedType = FDC3Event.Type.fromMessageType(messageType);
        Object details = buildEventDetails(resolvedType, payload);
        handler.handleEvent(new FDC3Event(resolvedType, details));
    }

    private static Object buildEventDetails(FDC3Event.Type type, Map<String, Object> payload) {
        if (type == FDC3Event.Type.USER_CHANNEL_CHANGED) {
            Map<String, Object> details = new HashMap<>();
            Object currentChannelId = payload.get("currentChannelId");
            if (currentChannelId == null) {
                currentChannelId = payload.get("newChannelId");
            }
            details.put("currentChannelId", currentChannelId);
            return details;
        }
        if (type == FDC3Event.Type.CONTEXT_CLEARED) {
            Map<String, Object> details = new HashMap<>();
            details.put("channelId", payload.get("channelId"));
            details.put("contextType", payload.get("contextType"));
            return details;
        }
        return payload;
    }

}
