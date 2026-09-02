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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.channel.PrivateChannel;
import org.finos.fdc3.api.errors.ChannelError;
import org.finos.fdc3.api.types.ContextHandler;
import org.finos.fdc3.api.types.Listener;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.api.types.EventHandler;
import org.finos.fdc3.api.types.FDC3Event;
import org.finos.fdc3.proxy.listeners.AbstractPrivateChannelEventListener;
import org.finos.fdc3.proxy.listeners.DefaultContextListener;
import org.finos.fdc3.proxy.listeners.PrivateChannelAddContextEventListener;
import org.finos.fdc3.proxy.listeners.PrivateChannelDisconnectEventListener;
import org.finos.fdc3.proxy.listeners.PrivateChannelNullEventListener;
import org.finos.fdc3.proxy.listeners.PrivateChannelUnsubscribeEventListener;
import org.finos.fdc3.schema.*;

/**
 * Default implementation of a PrivateChannel.
 */
public class DefaultPrivateChannel extends DefaultChannel implements PrivateChannel {

    public DefaultPrivateChannel(Messaging messaging, long messageExchangeTimeout, String id) {
        super(messaging, messageExchangeTimeout, id, Channel.Type.Private, null);
    }

    @Override
    public CompletionStage<Listener> addEventListener(String type, EventHandler handler) {
        if ("contextCleared".equals(type)) {
            return super.addEventListener(type, handler);
        }

        if (type == null) {
            return addAllEventListener(handler);
        }

        AbstractPrivateChannelEventListener listener;
        switch (type) {
            case "addContextListener":
                listener = new PrivateChannelAddContextEventListener(messaging, messageExchangeTimeout, getId(), handler);
                break;
            case "unsubscribe":
                listener = new PrivateChannelUnsubscribeEventListener(messaging, messageExchangeTimeout, getId(), handler);
                break;
            case "disconnect":
                listener = new PrivateChannelDisconnectEventListener(messaging, messageExchangeTimeout, getId(), handler);
                break;
            default:
                throw new RuntimeException(ChannelError.InvalidArguments.toString());
        }

        return listener.register().thenApply(v -> listener);
    }

    private CompletionStage<Listener> addAllEventListener(EventHandler handler) {
        return super.addEventListener("contextCleared", handler).thenCompose(channelListener -> {
            PrivateChannelNullEventListener privateChannelListener = new PrivateChannelNullEventListener(
                    messaging, messageExchangeTimeout, getId(), handler);
            return privateChannelListener.register()
                    .thenApply(v -> (Listener) new Listener() {
                        @Override
                        public CompletionStage<Void> unsubscribe() {
                            return channelListener.unsubscribe()
                                    .thenCompose(ignored -> privateChannelListener.unsubscribe());
                        }
                    })
                    .exceptionallyCompose(ex -> channelListener.unsubscribe().thenCompose(ignored -> {
                        CompletableFuture<Listener> failed = new CompletableFuture<>();
                        failed.completeExceptionally(ex);
                        return failed;
                    }));
        });
    }

    @Override
    public CompletionStage<Listener> onAddContextListener(EventHandler handler) {
        PrivateChannelAddContextEventListener listener = new PrivateChannelAddContextEventListener(
                messaging, messageExchangeTimeout, getId(),
                event -> handler.handleEvent(new FDC3Event(FDC3Event.Type.ADD_CONTEXT_LISTENER, event.getDetails())));
        return listener.register().thenApply(v -> listener);
    }

    @Override
    public CompletionStage<Listener> onUnsubscribe(EventHandler handler) {
        PrivateChannelUnsubscribeEventListener listener = new PrivateChannelUnsubscribeEventListener(
                messaging, messageExchangeTimeout, getId(),
                event -> handler.handleEvent(new FDC3Event(FDC3Event.Type.ON_UNSUBSCRIBE, event.getDetails())));
        return listener.register().thenApply(v -> listener);
    }

    @Override
    public CompletionStage<Listener> onDisconnect(EventHandler handler) {
        PrivateChannelDisconnectEventListener listener = new PrivateChannelDisconnectEventListener(
                messaging, messageExchangeTimeout, getId(),
                event -> handler.handleEvent(new FDC3Event(FDC3Event.Type.ON_DISCONNECT, event.getDetails())));
        return listener.register().thenApply(v -> listener);
    }

    @Override
    public void disconnect() {
        PrivateChannelDisconnectRequest request = new PrivateChannelDisconnectRequest();
        request.setType(PrivateChannelDisconnectRequestType.PRIVATE_CHANNEL_DISCONNECT_REQUEST);
        request.setMeta(messaging.createMeta());

        PrivateChannelDisconnectRequestPayload payload = new PrivateChannelDisconnectRequestPayload();
        payload.setChannelID(getId());
        request.setPayload(payload);

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        try {
            messaging.<Map<String, Object>>exchange(requestMap, "privateChannelDisconnectResponse", messageExchangeTimeout)
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
