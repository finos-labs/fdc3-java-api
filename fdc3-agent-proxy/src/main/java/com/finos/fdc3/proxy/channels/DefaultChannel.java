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

package com.finos.fdc3.proxy.channels;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import com.finos.fdc3.api.channel.Channel;
import com.finos.fdc3.api.context.Context;
import com.finos.fdc3.api.metadata.DisplayMetadata;
import com.finos.fdc3.api.types.ContextHandler;
import com.finos.fdc3.api.types.Listener;
import com.finos.fdc3.proxy.Messaging;
import com.finos.fdc3.proxy.listeners.DefaultContextListener;

/**
 * Default implementation of a Channel.
 */
public class DefaultChannel implements Channel {

    protected final Messaging messaging;
    protected final long messageExchangeTimeout;
    private final String id;
    private final Type type;
    private final DisplayMetadata displayMetadataValue;

    public DefaultChannel(
            Messaging messaging,
            long messageExchangeTimeout,
            String id,
            Type type,
            DisplayMetadata displayMetadata) {
        this.messaging = messaging;
        this.messageExchangeTimeout = messageExchangeTimeout;
        this.id = id;
        this.type = type;
        this.displayMetadataValue = displayMetadata;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Optional<DisplayMetadata> displayMetadata() {
        return Optional.ofNullable(displayMetadataValue);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> broadcast(Context context) {
        Map<String, Object> request = new HashMap<>();
        request.put("meta", messaging.createMeta());
        request.put("type", "broadcastRequest");

        Map<String, Object> payload = new HashMap<>();
        payload.put("channelId", id);
        payload.put("context", context.toMap());
        request.put("payload", payload);

        return messaging.<Map<String, Object>>exchange(request, "broadcastResponse", messageExchangeTimeout)
                .thenApply(response -> null);
    }

    @Override
    public CompletionStage<Optional<Context>> getCurrentContext() {
        return getCurrentContext(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Optional<Context>> getCurrentContext(String contextType) {
        Map<String, Object> request = new HashMap<>();
        request.put("meta", messaging.createMeta());
        request.put("type", "getCurrentContextRequest");

        Map<String, Object> payload = new HashMap<>();
        payload.put("channelId", id);
        payload.put("contextType", contextType);
        request.put("payload", payload);

        return messaging.<Map<String, Object>>exchange(request, "getCurrentContextResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    Map<String, Object> responsePayload = (Map<String, Object>) response.get("payload");
                    Map<String, Object> contextMap = (Map<String, Object>) responsePayload.get("context");
                    if (contextMap != null) {
                        return Optional.of(Context.fromMap(contextMap));
                    }
                    return Optional.empty();
                });
    }

    @Override
    public CompletionStage<Listener> addContextListener(String contextType, ContextHandler handler) {
        return addContextListenerInner(contextType, handler);
    }

    protected CompletionStage<Listener> addContextListenerInner(String contextType, ContextHandler handler) {
        DefaultContextListener listener = new DefaultContextListener(
                messaging,
                messageExchangeTimeout,
                id,
                contextType,
                handler
        );
        return listener.register().thenApply(v -> listener);
    }
}

