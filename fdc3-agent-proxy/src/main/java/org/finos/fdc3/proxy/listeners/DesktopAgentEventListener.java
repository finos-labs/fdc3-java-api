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
import org.finos.fdc3.schema.FDC3EventType;

/**
 * Listener for Desktop Agent events.
 * Extends AbstractListener to handle registration/unregistration.
 */
public class DesktopAgentEventListener extends AbstractListener<EventHandler> {

    private final String eventType;

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
        validateEventType(eventType);
        this.eventType = eventType;
    }

    /**
     * Validates that the event type is supported.
     * Throws RuntimeException with "UnknownEventType" if not supported.
     */
    private static void validateEventType(String eventType) {
        if (eventType == null) {
            // null is allowed (listen to all events)
            return;
        }
        switch (eventType) {
            case "userChannelChanged":
                // Valid event type
                return;
            default:
                throw new RuntimeException("UnknownEventType");
        }
    }

    @Override
    protected Map<String, Object> buildSubscribeRequest() {
        Map<String, Object> request = new HashMap<>();
        Map<String, Object> payload = new HashMap<>();
        FDC3EventType fdc3EventType = toFDC3SchemaEventType(eventType);
        // Explicitly set type to null if eventType is null, otherwise use the enum value
        payload.put("type", fdc3EventType != null ? fdc3EventType.toValue() : null);
        request.put("payload", payload);
        return request;
    }

    @Override
    public boolean filter(Map<String, Object> message) {
        String type = (String) message.get("type");
        if (eventType == null) {
            // Listen to all events
            return type != null && type.endsWith("Event");
        }
        return getExpectedMessageType().equals(type);
    }

    /**
     * Maps FDC3 event types to their corresponding message types.
     * e.g., "userChannelChanged" -> "channelChangedEvent"
     */
    private String getExpectedMessageType() {
        if (eventType == null) {
            return null;
        }
        switch (eventType) {
            case "userChannelChanged":
                return "channelChangedEvent";
            default:
                throw new RuntimeException("UnknownEventType");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void action(Map<String, Object> message) {
        String messageType = (String) message.get("type");
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        
        // Restructure the event based on message type
        if ("channelChangedEvent".equals(messageType)) {
            // Restructure channelChangedEvent to userChannelChanged format
            Map<String, Object> details = new HashMap<>();
            String newChannelId = (String) payload.get("newChannelId");
            details.put("currentChannelId", newChannelId);
            
            FDC3Event event = new FDC3Event(FDC3Event.Type.USER_CHANNEL_CHANGED, details);
            handler.handleEvent(event);
        } else {
            // For other event types (currently unused but meeting the spec)
            // Convert message type to FDC3Event.Type
            FDC3Event.Type fdc3EventType = messageTypeToFDC3EventType(messageType);
            FDC3Event event = new FDC3Event(fdc3EventType, payload);
            handler.handleEvent(event);
        }
    }

    private FDC3EventType toFDC3SchemaEventType(String eventType) {
        if (eventType == null) {
            return null;
        }
        switch (eventType) {
            case "userChannelChanged":
                return FDC3EventType.USER_CHANNEL_CHANGED;
            default:
                throw new RuntimeException("UnknownEventType");
        }
    }

    private FDC3Event.Type toFDC3EventType(String eventType) {
        if (eventType == null) {
            return null;
        }
        switch (eventType) {
            case "userChannelChanged":
                return FDC3Event.Type.USER_CHANNEL_CHANGED;
            default:
                throw new RuntimeException("UnknownEventType");
        }
    }

    /**
     * Converts a message type (e.g., "channelChangedEvent") to the corresponding
     * FDC3Event.Type enum value (e.g., USER_CHANNEL_CHANGED).
     * This is used for messages received from the desktop agent.
     */
    private FDC3Event.Type messageTypeToFDC3EventType(String messageType) {
        if (messageType == null) {
            throw new RuntimeException("UnknownEventType");
        }
        switch (messageType) {
            case "channelChangedEvent":
                return FDC3Event.Type.USER_CHANNEL_CHANGED;
            default:
                // Currently unused but provided for future event types
                throw new RuntimeException("UnknownEventType");
        }
    }
}
