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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.types.ContextHandler;
import org.finos.fdc3.api.types.Listener;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.schema.*;

/**
 * Default implementation of a context listener.
 */
public class DefaultContextListener implements RegisterableListener, Listener {

    protected final Messaging messaging;
    protected final long messageExchangeTimeout;
    protected final String channelId;
    protected final String contextType;
    protected final ContextHandler handler;
    protected final String messageType;
    private final String id;

    public DefaultContextListener(
            Messaging messaging,
            long messageExchangeTimeout,
            String channelId,
            String contextType,
            ContextHandler handler) {
        this(messaging, messageExchangeTimeout, channelId, contextType, handler, "broadcastEvent");
    }

    public DefaultContextListener(
            Messaging messaging,
            long messageExchangeTimeout,
            String channelId,
            String contextType,
            ContextHandler handler,
            String messageType) {
        this.messaging = messaging;
        this.messageExchangeTimeout = messageExchangeTimeout;
        this.channelId = channelId;
        this.contextType = contextType;
        this.handler = handler;
        this.messageType = messageType;
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
        if (!messageType.equals(type)) {
            return false;
        }

        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        if (payload == null) {
            return false;
        }

        String msgChannelId = (String) payload.get("channelId");
        if (channelId != null && !channelId.equals(msgChannelId)) {
            return false;
        }

        Map<String, Object> context = (Map<String, Object>) payload.get("context");
        if (context == null) {
            return false;
        }

        String msgContextType = (String) context.get("type");
        return contextType == null || contextType.equals(msgContextType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void action(Map<String, Object> message) {
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        Map<String, Object> contextMap = (Map<String, Object>) payload.get("context");
        Context context = Context.fromMap(contextMap);
        handler.handleContext(context, null);
    }

    @Override
    public CompletionStage<Void> register() {
        AddContextListenerRequest request = new AddContextListenerRequest();
        request.setType(AddContextListenerRequestType.ADD_CONTEXT_LISTENER_REQUEST);
        request.setMeta(messaging.createMeta());

        AddContextListenerRequestPayload payload = new AddContextListenerRequestPayload();
        payload.setChannelID(channelId);
        payload.setContextType(contextType);
        request.setPayload(payload);

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        messaging.register(this);

        return messaging.<Map<String, Object>>exchange(requestMap, "addContextListenerResponse", messageExchangeTimeout)
                .thenApply(response -> null);
    }

    @Override
    public void unsubscribe() {
        messaging.unregister(id);
    }

    public CompletionStage<Void> unsubscribeAsync() {
        messaging.unregister(id);
        return CompletableFuture.completedFuture(null);
    }
}
