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
 * Listens to Channel-scoped events (currently {@code contextCleared}) for a specific Channel.
 * Registration is sent to the Desktop Agent over DACP with the Channel's id so the Desktop
 * Agent can route events to this app, and inbound events are additionally filtered locally by
 * {@code channelId} so that only events for this Channel are delivered to the handler.
 */
public class ChannelEventListener extends AbstractListener<EventHandler> {

    private final FDC3Event.Type type;
    private final String channelId;

    public ChannelEventListener(
            Messaging messaging,
            long messageExchangeTimeout,
            String type,
            String channelId,
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
        this.type = parseChannelEventType(type);
        this.channelId = channelId;
    }

    private static FDC3Event.Type parseChannelEventType(String type) {
        if (type == null) {
            return null;
        }
        try {
            FDC3Event.Type parsed = FDC3Event.Type.fromValue(type);
            if (parsed == FDC3Event.Type.CONTEXT_CLEARED) {
                return parsed;
            }
        } catch (IllegalArgumentException e) {
            // fall through
        }
        throw new RuntimeException("UnknownEventType");
    }

    @Override
    protected Map<String, Object> buildSubscribeRequest() {
        Map<String, Object> request = new HashMap<>();
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type != null ? type.toWireValue() : null);
        payload.put("channelId", channelId);
        request.put("payload", payload);
        return request;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean filter(Map<String, Object> message) {
        if (!FDC3Event.Type.CONTEXT_CLEARED.toMessageType().equals(message.get("type"))) {
            return false;
        }
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        return payload != null && channelId.equals(payload.get("channelId"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void action(Map<String, Object> message) {
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        Map<String, Object> details = new HashMap<>();
        details.put("channelId", payload.get("channelId"));
        details.put("contextType", payload.get("contextType"));
        handler.handleEvent(new FDC3Event(FDC3Event.Type.CONTEXT_CLEARED, details));
    }
}
