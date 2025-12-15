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
        this.eventType = eventType;
    }

    @Override
    protected Map<String, Object> buildSubscribeRequest() {
        Map<String, Object> request = new HashMap<>();
        Map<String, Object> payload = new HashMap<>();
        FDC3EventType fdc3EventType = toFDC3EventType(eventType);
        if (fdc3EventType != null) {
            payload.put("type", fdc3EventType.toValue());
        }
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

    private String getExpectedMessageType() {
        return eventType + "Event";
    }

    @Override
    @SuppressWarnings("unchecked")
    public void action(Map<String, Object> message) {
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        FDC3Event<Map<String, Object>> event = new FDC3Event<>(eventType, payload);
        handler.handleEvent(event);
    }

    private FDC3EventType toFDC3EventType(String eventType) {
        if (eventType == null) {
            return null;
        }
        switch (eventType) {
            case "userChannelChanged":
                return FDC3EventType.USER_CHANNEL_CHANGED;
            default:
                return null;
        }
    }
}
