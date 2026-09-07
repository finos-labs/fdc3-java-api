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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.types.EventHandler;
import org.finos.fdc3.api.types.FDC3Event;
import org.finos.fdc3.api.types.Listener;
import org.finos.fdc3.proxy.Messaging;

/**
 * Listener for channel events such as contextCleared.
 */
public class ChannelEventListener implements RegisterableListener, Listener {

    private final Messaging messaging;
    private final String type;
    private final String channelId;
    private final EventHandler handler;
    private final String id;

    public ChannelEventListener(Messaging messaging, String type, String channelId, EventHandler handler) {
        this.messaging = messaging;
        this.type = type;
        this.channelId = channelId;
        this.handler = handler;
        this.id = channelId + "-" + (type != null ? type : "all") + "-" + messaging.createUUID();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean filter(Map<String, Object> message) {
        if (!"contextClearedEvent".equals(message.get("type"))) {
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

    @Override
    public CompletionStage<Void> register() {
        messaging.register(this);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> unsubscribe() {
        messaging.unregister(id);
        return CompletableFuture.completedFuture(null);
    }
}
