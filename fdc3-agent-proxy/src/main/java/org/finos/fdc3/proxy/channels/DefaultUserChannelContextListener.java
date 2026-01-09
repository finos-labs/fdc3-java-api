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
import org.finos.fdc3.api.types.ContextHandler;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.proxy.listeners.DefaultContextListener;

/**
 * Context listener that tracks user channel changes.
 * This extends DefaultContextListener and adds support for changing channels
 * when the user changes their current user channel.
 */
public class DefaultUserChannelContextListener extends DefaultContextListener implements UserChannelContextListener {

    private final DefaultChannelSupport channelSupport;

    public DefaultUserChannelContextListener(
            DefaultChannelSupport channelSupport,
            Messaging messaging,
            long messageExchangeTimeout,
            String contextType,
            ContextHandler handler) {
        super(messaging, messageExchangeTimeout, null, contextType, handler, "broadcastEvent");
        this.channelSupport = channelSupport;
    }

    @Override
    public CompletionStage<Void> register() {
        return super.register().thenCompose(v -> {
            changeChannel();
            return CompletableFuture.completedFuture(null);
        });
    }

    /**
     * Called when the user channel changes. Gets the current context from the
     * current channel and notifies the handler if there is one.
     */
    @Override
    public void changeChannel() {
        Channel currentChannel = channelSupport.getCurrentChannelInternal();
        if (currentChannel != null) {
            currentChannel.getCurrentContext(contextType)
                    .thenAccept(contextOpt -> {
                        contextOpt.ifPresent(context -> handler.handleContext(context, null));
                    });
        }
    }

    @Override
    public CompletionStage<Void> unsubscribe() {
        channelSupport.removeUserChannelListener(this);
        return super.unsubscribe();
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

        // Check if on matching channel or open broadcast
        String msgChannelId = (String) payload.get("channelId");
        Channel currentChannel = channelSupport.getCurrentChannelInternal();
        boolean onMatchingChannel = currentChannel != null && currentChannel.getId().equals(msgChannelId);
        boolean openBroadcast = msgChannelId == null;

        if (!onMatchingChannel && !openBroadcast) {
            return false;
        }

        Map<String, Object> context = (Map<String, Object>) payload.get("context");
        if (context == null) {
            return false;
        }

        String msgContextType = (String) context.get("type");
        return contextType == null || contextType.equals(msgContextType);
    }
}

