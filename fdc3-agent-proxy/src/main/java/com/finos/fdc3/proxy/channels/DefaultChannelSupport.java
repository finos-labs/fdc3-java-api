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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import com.finos.fdc3.api.channel.Channel;
import com.finos.fdc3.api.channel.PrivateChannel;
import com.finos.fdc3.api.errors.ChannelError;
import com.finos.fdc3.api.metadata.DisplayMetadata;
import com.finos.fdc3.api.types.ContextHandler;
import com.finos.fdc3.api.types.EventHandler;
import com.finos.fdc3.api.types.Listener;
import com.finos.fdc3.proxy.Messaging;
import com.finos.fdc3.proxy.listeners.DesktopAgentEventListener;
import com.finos.fdc3.proxy.util.Logger;

/**
 * Default implementation of ChannelSupport.
 */
public class DefaultChannelSupport implements ChannelSupport {

    private final Messaging messaging;
    private final ChannelSelector channelSelector;
    private final long messageExchangeTimeout;
    private List<Channel> userChannels = null;
    private Channel currentChannel = null;

    public DefaultChannelSupport(Messaging messaging, ChannelSelector channelSelector, long messageExchangeTimeout) {
        this.messaging = messaging;
        this.channelSelector = channelSelector;
        this.messageExchangeTimeout = messageExchangeTimeout;

        // Set up channel change callback
        channelSelector.setChannelChangeCallback(channelId -> {
            Logger.debug("Channel selector reports channel changed: {}", channelId);
            if (channelId == null) {
                leaveUserChannel();
            } else {
                joinUserChannel(channelId);
            }
        });

        // Listen for channel changed events from the Desktop Agent
        addEventListener(event -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) event.getDetails();
            String newChannelId = details != null ? (String) details.get("currentChannelId") : null;
            Logger.debug("Desktop Agent reports channel changed: {}", newChannelId);

            getUserChannelsCached().thenAccept(channels -> {
                Channel theChannel = null;
                if (newChannelId != null) {
                    theChannel = channels.stream()
                            .filter(c -> newChannelId.equals(c.getId()))
                            .findFirst()
                            .orElse(null);

                    if (theChannel == null) {
                        Logger.debug("Unknown user channel, querying Desktop Agent: {}", newChannelId);
                        getUserChannels().thenAccept(updatedChannels -> {
                            Channel foundChannel = updatedChannels.stream()
                                    .filter(c -> newChannelId.equals(c.getId()))
                                    .findFirst()
                                    .orElse(null);
                            if (foundChannel == null) {
                                Logger.warn("Received user channel update with unknown user channel: {}", newChannelId);
                            }
                            currentChannel = foundChannel;
                            channelSelector.updateChannel(newChannelId, updatedChannels);
                        });
                        return;
                    }
                }
                currentChannel = theChannel;
                channelSelector.updateChannel(theChannel != null ? theChannel.getId() : null, channels);
            });
        }, "userChannelChanged");
    }

    @Override
    public CompletionStage<Listener> addEventListener(EventHandler handler, String type) {
        DesktopAgentEventListener listener = new DesktopAgentEventListener(
                messaging, messageExchangeTimeout, type, handler);
        return listener.register().thenApply(v -> listener);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Channel> getUserChannel() {
        Map<String, Object> request = new HashMap<>();
        request.put("meta", messaging.createMeta());
        request.put("type", "getCurrentChannelRequest");
        request.put("payload", new HashMap<>());

        return messaging.<Map<String, Object>>exchange(request, "getCurrentChannelResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    Map<String, Object> payload = (Map<String, Object>) response.get("payload");
                    Map<String, Object> channel = (Map<String, Object>) payload.get("channel");

                    if (channel == null) {
                        return null;
                    }

                    String id = (String) channel.get("id");
                    Map<String, Object> displayMetadataMap = (Map<String, Object>) channel.get("displayMetadata");
                    DisplayMetadata displayMetadata = displayMetadataMap != null
                            ? DisplayMetadata.fromMap(displayMetadataMap)
                            : null;

                    return new DefaultChannel(messaging, messageExchangeTimeout, id, Channel.Type.User, displayMetadata);
                });
    }

    private CompletionStage<List<Channel>> getUserChannelsCached() {
        if (userChannels != null) {
            return CompletableFuture.completedFuture(userChannels);
        }
        return getUserChannels();
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<List<Channel>> getUserChannels() {
        Map<String, Object> request = new HashMap<>();
        request.put("meta", messaging.createMeta());
        request.put("type", "getUserChannelsRequest");
        request.put("payload", new HashMap<>());

        return messaging.<Map<String, Object>>exchange(request, "getUserChannelsResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    Map<String, Object> payload = (Map<String, Object>) response.get("payload");
                    List<Map<String, Object>> channelList = (List<Map<String, Object>>) payload.get("userChannels");

                    if (channelList == null) {
                        userChannels = new ArrayList<>();
                        return userChannels;
                    }

                    userChannels = channelList.stream()
                            .map(c -> {
                                String id = (String) c.get("id");
                                Map<String, Object> displayMetadataMap = (Map<String, Object>) c.get("displayMetadata");
                                DisplayMetadata displayMetadata = displayMetadataMap != null
                                        ? DisplayMetadata.fromMap(displayMetadataMap)
                                        : null;
                                return (Channel) new DefaultChannel(
                                        messaging, messageExchangeTimeout, id, Channel.Type.User, displayMetadata);
                            })
                            .collect(Collectors.toList());

                    return userChannels;
                });
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Channel> getOrCreate(String id) {
        Map<String, Object> request = new HashMap<>();
        request.put("meta", messaging.createMeta());
        request.put("type", "getOrCreateChannelRequest");

        Map<String, Object> payload = new HashMap<>();
        payload.put("channelId", id);
        request.put("payload", payload);

        return messaging.<Map<String, Object>>exchange(request, "getOrCreateChannelResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    Map<String, Object> responsePayload = (Map<String, Object>) response.get("payload");
                    Map<String, Object> channel = (Map<String, Object>) responsePayload.get("channel");

                    if (channel == null) {
                        throw new RuntimeException(ChannelError.CreationFailed.toString());
                    }

                    Map<String, Object> displayMetadataMap = (Map<String, Object>) channel.get("displayMetadata");
                    DisplayMetadata displayMetadata = displayMetadataMap != null
                            ? DisplayMetadata.fromMap(displayMetadataMap)
                            : null;

                    return new DefaultChannel(messaging, messageExchangeTimeout, id, Channel.Type.App, displayMetadata);
                });
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<PrivateChannel> createPrivateChannel() {
        Map<String, Object> request = new HashMap<>();
        request.put("meta", messaging.createMeta());
        request.put("type", "createPrivateChannelRequest");
        request.put("payload", new HashMap<>());

        return messaging.<Map<String, Object>>exchange(request, "createPrivateChannelResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    Map<String, Object> payload = (Map<String, Object>) response.get("payload");
                    Map<String, Object> channel = (Map<String, Object>) payload.get("privateChannel");

                    if (channel == null) {
                        throw new RuntimeException(ChannelError.CreationFailed.toString());
                    }

                    String id = (String) channel.get("id");
                    return new DefaultPrivateChannel(messaging, messageExchangeTimeout, id);
                });
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> leaveUserChannel() {
        Map<String, Object> request = new HashMap<>();
        request.put("meta", messaging.createMeta());
        request.put("type", "leaveCurrentChannelRequest");
        request.put("payload", new HashMap<>());

        return messaging.<Map<String, Object>>exchange(request, "leaveCurrentChannelResponse", messageExchangeTimeout)
                .thenCompose(response -> {
                    currentChannel = null;
                    return getUserChannelsCached().thenAccept(channels -> {
                        channelSelector.updateChannel(null, channels);
                    });
                });
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> joinUserChannel(String id) {
        Map<String, Object> request = new HashMap<>();
        request.put("meta", messaging.createMeta());
        request.put("type", "joinUserChannelRequest");

        Map<String, Object> payload = new HashMap<>();
        payload.put("channelId", id);
        request.put("payload", payload);

        return messaging.<Map<String, Object>>exchange(request, "joinUserChannelResponse", messageExchangeTimeout)
                .thenCompose(response -> getUserChannelsCached())
                .thenAccept(channels -> {
                    currentChannel = channels.stream()
                            .filter(c -> id.equals(c.getId()))
                            .findFirst()
                            .orElse(null);

                    if (currentChannel == null) {
                        throw new RuntimeException(ChannelError.NoChannelFound.toString());
                    }

                    channelSelector.updateChannel(id, channels);
                });
    }

    @Override
    public CompletionStage<Listener> addContextListener(ContextHandler handler, String type) {
        UserChannelContextListener listener = new UserChannelContextListener(this, messaging, messageExchangeTimeout, type, handler);
        return listener.register().thenApply(v -> listener);
    }

    // Package-private for UserChannelContextListener access
    Channel getCurrentChannelInternal() {
        return currentChannel;
    }
}

