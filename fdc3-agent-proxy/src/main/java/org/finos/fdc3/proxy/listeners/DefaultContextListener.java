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

import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.types.ContextHandler;
import org.finos.fdc3.proxy.Messaging;

/**
 * Default implementation of a context listener.
 * Extends AbstractListener to handle registration/unregistration.
 */
public class DefaultContextListener extends AbstractListener<ContextHandler> {

    protected String channelId;
    protected final String contextType;
    protected final String messageType;

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
        this.contextType = contextType;
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
            // Get current context from the channel
            channel.getCurrentContext(contextType)
                .thenAccept(context -> {
                    if (context.isPresent()) {
                        handler.handleContext(context.get(), null);
                    }
                });
        }
    }

    @Override
    protected Map<String, Object> buildSubscribeRequest() {
        Map<String, Object> request = new HashMap<>();
        Map<String, Object> payload = new HashMap<>();
        payload.put("channelId", channelId);
        payload.put("contextType", contextType);
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
}
