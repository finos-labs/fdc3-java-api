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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.channel.PrivateChannel;
import org.finos.fdc3.api.types.ContextHandler;
import org.finos.fdc3.api.types.Listener;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.proxy.listeners.DefaultContextListener;
import org.finos.fdc3.proxy.listeners.PrivateChannelEventListener;

/**
 * Default implementation of a PrivateChannel.
 */
public class DefaultPrivateChannel extends DefaultChannel implements PrivateChannel {

    public DefaultPrivateChannel(Messaging messaging, long messageExchangeTimeout, String id) {
        super(messaging, messageExchangeTimeout, id, Channel.Type.Private, null);
    }

    @Override
    public Listener onAddContextListener(Consumer<Optional<String>> handler) {
        PrivateChannelEventListener listener = new PrivateChannelEventListener(
                messaging, messageExchangeTimeout, getId(), "addContextListener", 
                event -> handler.accept(Optional.ofNullable((String) event.getDetails())));
        listener.registerSync();
        return listener;
    }

    @Override
    public Listener onUnsubsrcibe(Consumer<Optional<String>> handler) {
        PrivateChannelEventListener listener = new PrivateChannelEventListener(
                messaging, messageExchangeTimeout, getId(), "unsubscribe",
                event -> handler.accept(Optional.ofNullable((String) event.getDetails())));
        listener.registerSync();
        return listener;
    }

    @Override
    public Listener onDisconnect(Runnable handler) {
        PrivateChannelEventListener listener = new PrivateChannelEventListener(
                messaging, messageExchangeTimeout, getId(), "disconnect",
                event -> handler.run());
        listener.registerSync();
        return listener;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void disconnect() {
        Map<String, Object> request = new HashMap<>();
        request.put("meta", messaging.createMeta());
        request.put("type", "privateChannelDisconnectRequest");

        Map<String, Object> payload = new HashMap<>();
        payload.put("channelId", getId());
        request.put("payload", payload);

        try {
            messaging.<Map<String, Object>>exchange(request, "privateChannelDisconnectResponse", messageExchangeTimeout)
                    .toCompletableFuture().get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to disconnect private channel", e);
        }
    }

    @Override
    protected CompletionStage<Listener> addContextListenerInner(String contextType, ContextHandler handler) {
        DefaultContextListener listener = new DefaultContextListener(
                messaging,
                messageExchangeTimeout,
                getId(),
                contextType,
                handler
        );
        return listener.register().thenApply(v -> listener);
    }
}

