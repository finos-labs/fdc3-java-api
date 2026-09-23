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

package org.finos.fdc3.proxy.channels;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.metadata.AppProvidableContextMetadata;
import org.finos.fdc3.api.metadata.ContextMetadata;
import org.finos.fdc3.api.metadata.DisplayMetadata;
import org.finos.fdc3.api.types.ContextHandler;
import org.finos.fdc3.api.types.ContextWithMetadata;
import org.finos.fdc3.api.types.EventHandler;
import org.finos.fdc3.api.types.FDC3Event;
import org.finos.fdc3.api.types.Listener;
import org.finos.fdc3.api.errors.ChannelError;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.proxy.util.ContextMetadataMapper;
import org.finos.fdc3.proxy.listeners.ChannelEventListener;
import org.finos.fdc3.proxy.listeners.DefaultContextListener;
import org.finos.fdc3.schema.*;

/**
 * Default implementation of a Channel.
 */
@JsonAutoDetect(
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
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
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @Override
    @JsonIgnore
    public Type getType() {
        return type;
    }
    
    @JsonProperty("type")
    @JsonGetter("type")
    public String getTypeValue() {
        return type != null ? type.getValue() : null;
    }

    @Override
    @JsonProperty("displayMetadata")
    public DisplayMetadata getDisplayMetadata() {
        return displayMetadata;
    }

    @Override
    @JsonIgnore
    public CompletionStage<Void> broadcast(Context context) {
        return broadcast(context, null);
    }

    @Override
    @JsonIgnore
    public CompletionStage<Void> broadcast(Context context, AppProvidableContextMetadata metadata) {
        if (context == null || context.getType() == null) {
            return CompletableFuture.failedFuture(
                    new RuntimeException(ChannelError.MalformedContext.toString()));
        }

        BroadcastRequest request = new BroadcastRequest();
        request.setType(BroadcastRequestType.BROADCAST_REQUEST);
        request.setMeta(messaging.createMeta());

        BroadcastRequestPayload payload = new BroadcastRequestPayload();
        payload.setChannelID(id);
        payload.setContext(context);
        request.setPayload(payload);

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);
        @SuppressWarnings("unchecked")
        Map<String, Object> payloadMap = (Map<String, Object>) requestMap.get("payload");
        if (payloadMap != null && metadata != null) {
            payloadMap.put("metadata", ContextMetadataMapper.toWire(metadata));
        } else if (payloadMap != null) {
            payloadMap.remove("metadata");
        }

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
                    ContextWithMetadata parsed = parseCurrentContextResponse(response);
                    return parsed != null ? Optional.of(parsed.getContext()) : Optional.empty();
                });
    }

    @Override
    @JsonIgnore
    public CompletionStage<Optional<ContextWithMetadata>> getCurrentContextWithMetadata(String contextType) {
        GetCurrentContextRequest request = new GetCurrentContextRequest();
        request.setType(GetCurrentContextRequestType.GET_CURRENT_CONTEXT_REQUEST);
        request.setMeta(messaging.createMeta());

        GetCurrentContextRequestPayload payload = new GetCurrentContextRequestPayload();
        payload.setChannelID(id);
        payload.setContextType(contextType);
        request.setPayload(payload);

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        return messaging.<Map<String, Object>>exchange(requestMap, "getCurrentContextResponse", messageExchangeTimeout)
                .thenApply(response -> Optional.ofNullable(parseCurrentContextResponse(response)));
    }

    @Override
    @JsonIgnore
    public CompletionStage<Void> clearContext(String contextType) {
        ClearContextRequest request = new ClearContextRequest();
        request.setType(ClearContextRequestType.CLEAR_CONTEXT_REQUEST);
        request.setMeta(messaging.createMeta());

        ClearContextRequestPayload payload = new ClearContextRequestPayload();
        payload.setChannelID(id);
        payload.setContextType(contextType);
        request.setPayload(payload);

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        return messaging.<Map<String, Object>>exchange(requestMap, "clearContextResponse", messageExchangeTimeout)
                .thenApply(response -> null);
    }

    @Override
    @JsonIgnore
    public CompletionStage<Listener> addContextListener(String contextType, ContextHandler handler) {
        if (handler == null) {
            return CompletableFuture.failedFuture(
                    new RuntimeException(ChannelError.InvalidArguments.toString()));
        }
        List<String> types = contextType == null ? null : List.of(contextType);
        return addContextListenerInner(types, handler);
    }

    @Override
    @JsonIgnore
    public CompletionStage<Listener> addContextListener(List<String> contextTypes, ContextHandler handler) {
        // null is the String-overload "all types" signal; reflective callers cannot
        // distinguish the overloads, so treat null here the same way.
        if (contextTypes == null) {
            return addContextListener((String) null, handler);
        }
        if (handler == null || contextTypes.isEmpty() || contextTypes.stream().anyMatch(t -> t == null)) {
            return CompletableFuture.failedFuture(
                    new RuntimeException(ChannelError.InvalidArguments.toString()));
        }
        return addContextListenerInner(contextTypes, handler);
    }

    protected CompletionStage<Listener> addContextListenerInner(
            List<String> contextTypes, ContextHandler handler) {
        DefaultContextListener listener = new DefaultContextListener(
                messaging,
                messageExchangeTimeout,
                id,
                contextTypes,
                handler
        );
        return listener.register().thenApply(v -> listener);
    }

    @Override
    public CompletionStage<Listener> addEventListener(String type, EventHandler handler) {
        if (type != null) {
            try {
                if (FDC3Event.Type.fromValue(type) != FDC3Event.Type.CONTEXT_CLEARED) {
                    return CompletableFuture.failedFuture(
                            new RuntimeException(ChannelError.InvalidArguments.toString()));
                }
            } catch (IllegalArgumentException e) {
                return CompletableFuture.failedFuture(
                        new RuntimeException(ChannelError.InvalidArguments.toString()));
            }
        }
        ChannelEventListener listener = new ChannelEventListener(
                messaging, messageExchangeTimeout, type, id, handler);
        return listener.register().thenApply(v -> listener);
    }

    /**
     * Parses a getCurrentContextResponse payload, mirroring TypeScript
     * {@code parseCurrentContextResponse}.
     */
    @SuppressWarnings("unchecked")
    private ContextWithMetadata parseCurrentContextResponse(Map<String, Object> response) {
        Map<String, Object> responsePayload = (Map<String, Object>) response.get("payload");
        if (responsePayload == null) {
            throw new RuntimeException(ChannelError.MalformedContext.toString());
        }

        boolean hasContextKey = responsePayload.containsKey("context");
        Object ctxRaw = responsePayload.get("context");
        boolean hasMetadataKey = responsePayload.containsKey("metadata");
        Object metadataRaw = responsePayload.get("metadata");

        if (ctxRaw == null) {
            if (!hasContextKey) {
                // missing context key — MalformedContext (TS: context === undefined)
                throw new RuntimeException(ChannelError.MalformedContext.toString());
            }
            // context explicitly null: only valid when metadata is also explicitly null
            // (TS treats absent metadata as undefined, which is !== null → MalformedContext)
            if (!hasMetadataKey || metadataRaw != null) {
                throw new RuntimeException(ChannelError.MalformedContext.toString());
            }
            return null;
        }

        if (!hasMetadataKey || metadataRaw == null) {
            throw new RuntimeException(ChannelError.MalformedContext.toString());
        }

        Context context;
        if (ctxRaw instanceof Context) {
            context = (Context) ctxRaw;
        } else if (ctxRaw instanceof Map) {
            context = Context.fromMap((Map<String, Object>) ctxRaw);
        } else {
            throw new RuntimeException(ChannelError.MalformedContext.toString());
        }

        GetCurrentContextResponse typedResponse = messaging.getConverter()
                .convertValue(response, GetCurrentContextResponse.class);
        Map<String, Object> payloadMetadata = metadataRaw instanceof Map
                ? (Map<String, Object>) metadataRaw
                : null;
        Object messageTimestamp = typedResponse.getMeta() != null
                ? typedResponse.getMeta().getTimestamp()
                : null;
        ContextMetadata metadata = ContextMetadataMapper.fromWire(
                payloadMetadata, messageTimestamp, ContextMetadataMapper.MissingTraceId.EMPTY);
        return new ContextWithMetadata(context, metadata);
    }
}
