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

import org.finos.fdc3.api.types.EventHandler;
import org.finos.fdc3.api.types.FDC3Event;
import org.finos.fdc3.api.types.Listener;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.schema.*;

/**
 * Listener for Desktop Agent events.
 */
public class DesktopAgentEventListener implements RegisterableListener, Listener {

    private final Messaging messaging;
    private final long messageExchangeTimeout;
    private final String eventType;
    private final EventHandler handler;
    private final String id;

    public DesktopAgentEventListener(
            Messaging messaging,
            long messageExchangeTimeout,
            String eventType,
            EventHandler handler) {
        this.messaging = messaging;
        this.messageExchangeTimeout = messageExchangeTimeout;
        this.eventType = eventType;
        this.handler = handler;
        this.id = messaging.createUUID();
    }

    @Override
    public String getId() {
        return id;
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

    @Override
    public CompletionStage<Void> register() {
        AddEventListenerRequest request = new AddEventListenerRequest();
        request.setType(AddEventListenerRequestType.ADD_EVENT_LISTENER_REQUEST);
        request.setMeta(messaging.createMeta());

        AddEventListenerRequestPayload payload = new AddEventListenerRequestPayload();
        payload.setType(toFDC3EventType(eventType));
        request.setPayload(payload);

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        messaging.register(this);

        return messaging.<Map<String, Object>>exchange(requestMap, "addEventListenerResponse", messageExchangeTimeout)
                .thenApply(response -> null);
    }

    @Override
    public void unsubscribe() {
        messaging.unregister(id);
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
