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

package org.finos.fdc3.proxy.channels;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.metadata.DisplayMetadata;
import org.finos.fdc3.api.types.ContextHandler;
import org.finos.fdc3.api.types.Listener;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.proxy.listeners.DefaultContextListener;
import org.finos.fdc3.schema.*;

/**
 * Default implementation of a Channel.
 */
public class DefaultChannel implements Channel {

    @JsonIgnore
    protected final Messaging messaging;
    @JsonIgnore
    protected final long messageExchangeTimeout;
    private final String id;
    private final Type type;
    @JsonIgnore
    private final DisplayMetadata displayMetadata;

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
        this.displayMetadata = displayMetadata;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    @JsonIgnore
    public Type getType() {
        return type;
    }
    
    @JsonGetter("type")
    public String getTypeValue() {
        return type != null ? type.getValue() : null;
    }

    @Override
    public DisplayMetadata getDisplayMetadata() {
        return displayMetadata;
    }

    @Override
    @JsonIgnore
    public CompletionStage<Void> broadcast(Context context) {
        BroadcastRequest request = new BroadcastRequest();
        request.setType(BroadcastRequestType.BROADCAST_REQUEST);
        request.setMeta(messaging.createMeta());

        BroadcastRequestPayload payload = new BroadcastRequestPayload();
        payload.setChannelID(id);
        payload.setContext(context);
        request.setPayload(payload);

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        return messaging.<Map<String, Object>>exchange(requestMap, "broadcastResponse", messageExchangeTimeout)
                .thenApply(response -> null);
    }

    @Override
    @JsonIgnore
    public CompletionStage<Optional<Context>> getCurrentContext() {
        return getCurrentContext(null);
    }

    @Override
    @JsonIgnore
    public CompletionStage<Optional<Context>> getCurrentContext(String contextType) {
        GetCurrentContextRequest request = new GetCurrentContextRequest();
        request.setType(GetCurrentContextRequestType.GET_CURRENT_CONTEXT_REQUEST);
        request.setMeta(messaging.createMeta());

        GetCurrentContextRequestPayload payload = new GetCurrentContextRequestPayload();
        payload.setChannelID(id);
        payload.setContextType(contextType);
        request.setPayload(payload);

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        return messaging.<Map<String, Object>>exchange(requestMap, "getCurrentContextResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    GetCurrentContextResponse typedResponse = messaging.getConverter()
                            .convertValue(response, GetCurrentContextResponse.class);

                    if (typedResponse.getPayload() != null &&
                        typedResponse.getPayload().getContext() != null) {
                        return Optional.of(typedResponse.getPayload().getContext());
                    }
                    return Optional.empty();
                });
    }

    @Override
    @JsonIgnore
    public CompletionStage<Listener> addContextListener(String contextType, ContextHandler handler) {
        return addContextListenerInner(contextType, handler);
    }

    @Override
    @JsonIgnore
    @Deprecated
    public CompletionStage<Listener> addContextListener(ContextHandler handler) {
        return addContextListener(null, handler);
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
