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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.channel.PrivateChannel;
import org.finos.fdc3.api.errors.ChannelError;
import org.finos.fdc3.api.types.ContextHandler;
import org.finos.fdc3.api.types.EventHandler;
import org.finos.fdc3.api.types.Listener;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.proxy.listeners.DesktopAgentEventListener;
import org.finos.fdc3.proxy.util.Logger;
import org.finos.fdc3.schema.CreatePrivateChannelRequest;
import org.finos.fdc3.schema.CreatePrivateChannelRequestPayload;
import org.finos.fdc3.schema.CreatePrivateChannelRequestType;
import org.finos.fdc3.schema.CreatePrivateChannelResponse;
import org.finos.fdc3.schema.GetCurrentChannelRequest;
import org.finos.fdc3.schema.GetCurrentChannelRequestPayload;
import org.finos.fdc3.schema.GetCurrentChannelRequestType;
import org.finos.fdc3.schema.GetCurrentChannelResponse;
import org.finos.fdc3.schema.GetOrCreateChannelRequest;
import org.finos.fdc3.schema.GetOrCreateChannelRequestPayload;
import org.finos.fdc3.schema.GetOrCreateChannelRequestType;
import org.finos.fdc3.schema.GetOrCreateChannelResponse;
import org.finos.fdc3.schema.GetUserChannelsRequest;
import org.finos.fdc3.schema.GetUserChannelsRequestPayload;
import org.finos.fdc3.schema.GetUserChannelsRequestType;
import org.finos.fdc3.schema.GetUserChannelsResponse;
import org.finos.fdc3.schema.JoinUserChannelRequest;
import org.finos.fdc3.schema.JoinUserChannelRequestPayload;
import org.finos.fdc3.schema.JoinUserChannelRequestType;
import org.finos.fdc3.schema.LeaveCurrentChannelRequest;
import org.finos.fdc3.schema.LeaveCurrentChannelRequestPayload;
import org.finos.fdc3.schema.LeaveCurrentChannelRequestType;

/**
 * Default implementation of ChannelSupport.
 */
public class DefaultChannelSupport implements ChannelSupport {

    private final Messaging messaging;
    private final ChannelSelector channelSelector;
    private final long messageExchangeTimeout;
    private List<Channel> userChannels = null;
    private Channel currentChannel = null;
    private final List<UserChannelContextListener> userChannelListeners = new ArrayList<>();

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

            getUserChannelsCached().thenCompose(channels -> {
                Channel theChannel = null;
                
                // If there's a newChannelId, retrieve details of the channel
                if (newChannelId != null) {
                    theChannel = channels.stream()
                            .filter(c -> newChannelId.equals(c.getId()))
                            .findFirst()
                            .orElse(null);

                    if (theChannel == null) {
                        // Channel not found - query user channels in case they have changed
                        Logger.debug("Unknown user channel, querying Desktop Agent for updated user channels: {}", newChannelId);
                        return getUserChannels().thenApply(updatedChannels -> {
                            Channel foundChannel = updatedChannels.stream()
                                    .filter(c -> newChannelId.equals(c.getId()))
                                    .findFirst()
                                    .orElse(null);
                            
                            if (foundChannel == null) {
                                Logger.warn("Received user channel update with unknown user channel (user channel listeners will not work): {}", newChannelId);
                            }
                            
                            currentChannel = foundChannel;
                            channelSelector.updateChannel(foundChannel != null ? foundChannel.getId() : null, updatedChannels);
                            return null;
                        });
                    }
                }
                
                // Channel found in cache or newChannelId is null
                currentChannel = theChannel;
                channelSelector.updateChannel(theChannel != null ? theChannel.getId() : null, channels);
                return CompletableFuture.completedFuture(null);
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
    public CompletionStage<Channel> getUserChannel() {
        GetCurrentChannelRequest request = new GetCurrentChannelRequest();
        request.setType(GetCurrentChannelRequestType.GET_CURRENT_CHANNEL_REQUEST);
        request.setMeta(messaging.createMeta());
        request.setPayload(new GetCurrentChannelRequestPayload());

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        return messaging.<Map<String, Object>>exchange(requestMap, "getCurrentChannelResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    GetCurrentChannelResponse typedResponse = messaging.getConverter()
                            .convertValue(response, GetCurrentChannelResponse.class);

                    if (typedResponse.getPayload() == null ||
                        typedResponse.getPayload().getChannel() == null) {
                        return null;
                    }

                    org.finos.fdc3.schema.Channel schemaChannel = typedResponse.getPayload().getChannel();
                    // Schema now uses fdc3-standard DisplayMetadata directly
                    return new DefaultChannel(messaging, messageExchangeTimeout, 
                            schemaChannel.getID(), Channel.Type.User, schemaChannel.getDisplayMetadata());
                });
    }

    private CompletionStage<List<Channel>> getUserChannelsCached() {
        if (userChannels != null) {
            return CompletableFuture.completedFuture(userChannels);
        }
        return getUserChannels();
    }

    @Override
    public CompletionStage<List<Channel>> getUserChannels() {
        GetUserChannelsRequest request = new GetUserChannelsRequest();
        request.setType(GetUserChannelsRequestType.GET_USER_CHANNELS_REQUEST);
        request.setMeta(messaging.createMeta());
        request.setPayload(new GetUserChannelsRequestPayload());

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        return messaging.<Map<String, Object>>exchange(requestMap, "getUserChannelsResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    GetUserChannelsResponse typedResponse = messaging.getConverter()
                            .convertValue(response, GetUserChannelsResponse.class);

                    if (typedResponse.getPayload() == null ||
                        typedResponse.getPayload().getUserChannels() == null) {
                        userChannels = new ArrayList<>();
                        return userChannels;
                    }

                    // Schema now uses fdc3-standard DisplayMetadata directly
                    userChannels = Arrays.stream(typedResponse.getPayload().getUserChannels())
                            .map(c -> (Channel) new DefaultChannel(
                                        messaging, messageExchangeTimeout, c.getID(), Channel.Type.User, c.getDisplayMetadata()))
                            .collect(Collectors.toList());

                    return userChannels;
                });
    }

    @Override
    public CompletionStage<Channel> getOrCreate(String id) {
        GetOrCreateChannelRequest request = new GetOrCreateChannelRequest();
        request.setType(GetOrCreateChannelRequestType.GET_OR_CREATE_CHANNEL_REQUEST);
        request.setMeta(messaging.createMeta());

        GetOrCreateChannelRequestPayload payload = new GetOrCreateChannelRequestPayload();
        payload.setChannelID(id);
        request.setPayload(payload);

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        return messaging.<Map<String, Object>>exchange(requestMap, "getOrCreateChannelResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    GetOrCreateChannelResponse typedResponse = messaging.getConverter()
                            .convertValue(response, GetOrCreateChannelResponse.class);

                    if (typedResponse.getPayload() == null ||
                        typedResponse.getPayload().getChannel() == null) {
                        throw new RuntimeException(ChannelError.CreationFailed.toString());
                    }

                    org.finos.fdc3.schema.Channel schemaChannel = typedResponse.getPayload().getChannel();
                    // Schema now uses fdc3-standard DisplayMetadata directly
                    return new DefaultChannel(messaging, messageExchangeTimeout, id, Channel.Type.App, schemaChannel.getDisplayMetadata());
                });
    }

    @Override
    public CompletionStage<PrivateChannel> createPrivateChannel() {
        CreatePrivateChannelRequest request = new CreatePrivateChannelRequest();
        request.setType(CreatePrivateChannelRequestType.CREATE_PRIVATE_CHANNEL_REQUEST);
        request.setMeta(messaging.createMeta());
        request.setPayload(new CreatePrivateChannelRequestPayload());

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        return messaging.<Map<String, Object>>exchange(requestMap, "createPrivateChannelResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    CreatePrivateChannelResponse typedResponse = messaging.getConverter()
                            .convertValue(response, CreatePrivateChannelResponse.class);

                    if (typedResponse.getPayload() == null ||
                        typedResponse.getPayload().getPrivateChannel() == null) {
                        throw new RuntimeException(ChannelError.CreationFailed.toString());
                    }

                    String id = typedResponse.getPayload().getPrivateChannel().getID();
                    return new DefaultPrivateChannel(messaging, messageExchangeTimeout, id);
                });
    }

    @Override
    public CompletionStage<Void> leaveUserChannel() {
        LeaveCurrentChannelRequest request = new LeaveCurrentChannelRequest();
        request.setType(LeaveCurrentChannelRequestType.LEAVE_CURRENT_CHANNEL_REQUEST);
        request.setMeta(messaging.createMeta());
        request.setPayload(new LeaveCurrentChannelRequestPayload());

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        return messaging.<Map<String, Object>>exchange(requestMap, "leaveCurrentChannelResponse", messageExchangeTimeout)
                .thenCompose(response -> {
                    currentChannel = null;
                    return getUserChannelsCached().thenAccept(channels -> {
                        channelSelector.updateChannel(null, channels);
                    });
                });
    }

    @Override
    public CompletionStage<Void> joinUserChannel(String id) {
        JoinUserChannelRequest request = new JoinUserChannelRequest();
        request.setType(JoinUserChannelRequestType.JOIN_USER_CHANNEL_REQUEST);
        request.setMeta(messaging.createMeta());

        JoinUserChannelRequestPayload payload = new JoinUserChannelRequestPayload();
        payload.setChannelID(id);
        request.setPayload(payload);

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        return messaging.<Map<String, Object>>exchange(requestMap, "joinUserChannelResponse", messageExchangeTimeout)
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
                    
                    // Notify all user channel listeners of the channel change
                    for (UserChannelContextListener listener : userChannelListeners) {
                        listener.changeChannel();
                    }
                });
    }

    @Override
    public CompletionStage<Listener> addContextListener(ContextHandler handler, String type) {
        DefaultUserChannelContextListener listener = new DefaultUserChannelContextListener(this, messaging, messageExchangeTimeout, type, handler);
        userChannelListeners.add(listener);
        return listener.register().thenApply(v -> listener);
    }

    // Package-private for UserChannelContextListener access
    Channel getCurrentChannelInternal() {
        return currentChannel;
    }

    // Package-private for UserChannelContextListener to remove itself on unsubscribe
    void removeUserChannelListener(UserChannelContextListener listener) {
        userChannelListeners.remove(listener);
    }
}
