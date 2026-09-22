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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.metadata.ContextMetadata;
import org.finos.fdc3.api.types.ContextHandler;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.proxy.util.ContextMetadataMapper;
import org.finos.fdc3.proxy.util.Logger;

/**
 * Default implementation of a context listener.
 * Extends AbstractListener to handle registration/unregistration.
 * <p>
 * Context types are always held as a {@link List}{@code String}, or {@code null} for all types.
 * On the wire, a single type (or all-types) uses {@code contextType}; multiple types use
 * {@code contextTypes}.
 */
public class DefaultContextListener extends AbstractListener<ContextHandler> {

    protected String channelId;
    /**
     * Types to match, or {@code null} for all types.
     */
    protected final List<String> contextTypes;
    protected final String messageType;

    public DefaultContextListener(
            Messaging messaging,
            long messageExchangeTimeout,
            String channelId,
            List<String> contextTypes,
            ContextHandler handler) {
        this(messaging, messageExchangeTimeout, channelId, contextTypes, handler, "broadcastEvent");
    }

    public DefaultContextListener(
            Messaging messaging,
            long messageExchangeTimeout,
            String channelId,
            List<String> contextTypes,
            ContextHandler handler,
            String messageType) {
        super(
            messaging,
            messageExchangeTimeout,
            handler,
            "addContextListenerRequest",
            "addContextListenerResponse",
            "contextListenerUnsubscribeRequest",
            "contextListenerUnsubscribeResponse"
        );
        this.channelId = channelId;
        this.contextTypes = contextTypes;
        this.messageType = messageType;
    }

    /**
     * Update the channel this listener is listening to. This is used for non-user
     * channel listeners (e.g., app channels, private channels).
     *
     * @param channel the new channel to listen to
     */
    public void changeChannel(Channel channel) {
        if (channel == null) {
            this.channelId = null;
        } else {
            this.channelId = channel.getId();
            replayCurrentContext(channel)
                .exceptionally(error -> {
                    Logger.error("Failed to replay current context of type {} from channel {}",
                            contextTypes, this.channelId, error);
                    return null;
                });
        }
    }

    /**
     * Replays matching current context from the given channel into the handler.
     */
    protected CompletionStage<Void> replayCurrentContext(Channel channel) {
        if (contextTypes != null) {
            CompletionStage<Void> chain = CompletableFuture.completedFuture(null);
            for (String ct : contextTypes) {
                chain = chain.thenCompose(ignored ->
                        channel.getCurrentContextWithMetadata(ct).thenAccept(result -> {
                            result.ifPresent(cwm -> handler.handleContext(cwm.getContext(), cwm.getMetadata()));
                        }));
            }
            return chain;
        }
        return channel.getCurrentContextWithMetadata(null)
                .thenAccept(result -> {
                    result.ifPresent(cwm -> handler.handleContext(cwm.getContext(), cwm.getMetadata()));
                });
    }

    protected boolean matchesContextType(String msgContextType) {
        if (contextTypes == null) {
            return true;
        }
        return contextTypes.contains(msgContextType);
    }

    @Override
    protected Map<String, Object> buildSubscribeRequest() {
        Map<String, Object> request = new HashMap<>();
        Map<String, Object> payload = new HashMap<>();
        payload.put("channelId", channelId);
        if (contextTypes != null && contextTypes.size() > 1) {
            payload.put("contextTypes", contextTypes);
        } else {
            payload.put("contextType", contextTypes == null ? null : contextTypes.get(0));
        }
        request.put("payload", payload);
        return request;
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
        return matchesContextType(msgContextType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void action(Map<String, Object> message) {
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        Map<String, Object> contextMap = (Map<String, Object>) payload.get("context");
        Context context = Context.fromMap(contextMap);
        Map<String, Object> messageMeta = (Map<String, Object>) message.get("meta");
        Map<String, Object> payloadMetadata = (Map<String, Object>) payload.get("metadata");
        Object messageTimestamp = messageMeta != null ? messageMeta.get("timestamp") : null;
        ContextMetadata metadata = ContextMetadataMapper.fromWire(
                payloadMetadata, messageTimestamp, messageMeta,
                ContextMetadataMapper.MissingTraceId.LEAVE_ABSENT);
        handler.handleContext(context, metadata);
    }
}
