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

package org.finos.fdc3.proxy;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.finos.fdc3.api.DesktopAgent;
import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.channel.PrivateChannel;
import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.metadata.AppIntent;
import org.finos.fdc3.api.metadata.AppMetadata;
import org.finos.fdc3.api.metadata.ImplementationMetadata;
import org.finos.fdc3.api.metadata.IntentResolution;
import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.api.types.ContextHandler;
import org.finos.fdc3.api.types.EventHandler;
import org.finos.fdc3.api.types.IntentHandler;
import org.finos.fdc3.api.types.Listener;
import org.finos.fdc3.proxy.apps.AppSupport;
import org.finos.fdc3.proxy.channels.ChannelSupport;
import org.finos.fdc3.proxy.heartbeat.HeartbeatSupport;
import org.finos.fdc3.proxy.intents.IntentSupport;

/**
 * Desktop Agent Proxy implementation.
 * <p>
 * This splits out the functionality of the desktop agent into
 * app, channels and intents concerns.
 */
public class DesktopAgentProxy implements DesktopAgent, Connectable {

    private final HeartbeatSupport heartbeat;
    private final ChannelSupport channels;
    private final IntentSupport intents;
    private final AppSupport apps;
    private final List<Connectable> connectables;

    public DesktopAgentProxy(
            HeartbeatSupport heartbeat,
            ChannelSupport channels,
            IntentSupport intents,
            AppSupport apps,
            List<Connectable> connectables) {
        this.heartbeat = heartbeat;
        this.intents = intents;
        this.channels = channels;
        this.apps = apps;
        this.connectables = connectables;
    }

    @Override
    public CompletionStage<Listener> addEventListener(String type, EventHandler handler) {
        return channels.addEventListener(handler, type);
    }

    @Override
    public CompletionStage<ImplementationMetadata> getInfo() {
        return apps.getImplementationMetadata();
    }

    @Override
    public CompletionStage<Void> broadcast(Context context) {
        return channels.getUserChannel()
                .thenCompose(channel -> {
                    if (channel != null) {
                        return channel.broadcast(context);
                    } else {
                        return CompletableFuture.completedFuture(null);
                    }
                });
    }

    @Override
    public CompletionStage<Listener> addContextListener(String contextType, ContextHandler handler) {
        return channels.addContextListener(handler, contextType);
    }
    
    @Override
    public CompletionStage<Listener> addContextListener(ContextHandler handler) {
        return channels.addContextListener(handler, null);
    }

    @Override
    public CompletionStage<List<Channel>> getUserChannels() {
        return channels.getUserChannels();
    }
    
    @Deprecated
    public CompletionStage<List<Channel>> getSystemChannels() {
        return channels.getUserChannels();
    }

    @Override
    public CompletionStage<Channel> getOrCreateChannel(String channelId) {
        return channels.getOrCreate(channelId);
    }

    @Override
    public CompletionStage<PrivateChannel> createPrivateChannel() {
        return channels.createPrivateChannel();
    }

    @Override
    public CompletionStage<Void> leaveCurrentChannel() {
        return channels.leaveUserChannel();
    }

    @Override
    public CompletionStage<Void> joinUserChannel(String channelId) {
        return channels.joinUserChannel(channelId);
    }
    
    public CompletionStage<Void> joinChannel(String channelId) {
        return channels.joinUserChannel(channelId);
    }

    @Override
    public CompletionStage<Optional<Channel>> getCurrentChannel() {
        return channels.getUserChannel()
                .thenApply(Optional::ofNullable);
    }

    @Override
    public CompletionStage<AppIntent> findIntent(String intent, Context context, String resultType) {
        return intents.findIntent(intent, context, resultType);
    }

    @Override
    public CompletionStage<List<AppIntent>> findIntentsByContext(Context context, String resultType) {
        return intents.findIntentsByContext(context);
    }

    @Override
    public CompletionStage<IntentResolution> raiseIntent(String intent, Context context, AppIdentifier app) {
        return intents.raiseIntent(intent, context, app);
    }

    @Override
    public CompletionStage<Listener> addIntentListener(String intent, IntentHandler handler) {
        return intents.addIntentListener(intent, handler);
    }

    @Override
    public CompletionStage<IntentResolution> raiseIntentForContext(Context context, AppIdentifier app) {
        return intents.raiseIntentForContext(context, app);
    }

    @Override
    public CompletionStage<AppIdentifier> open(AppIdentifier app, Context context) {
        return apps.open(app, context);
    }

    @Override
    @Deprecated
    public CompletionStage<AppIdentifier> open(String name, Context context) {
        return apps.open(name, context);
    }

    @Override
    public CompletionStage<List<AppIdentifier>> findInstances(AppIdentifier app) {
        return apps.findInstances(app);
    }

    @Override
    public CompletionStage<AppMetadata> getAppMetadata(AppIdentifier app) {
        return apps.getAppMetadata(app);
    }

    @Override
    public CompletionStage<Void> disconnect() {
        List<CompletableFuture<Void>> futures = connectables.stream()
                .map(c -> c.disconnect().toCompletableFuture())
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Override
    public CompletionStage<Void> connect() {
        List<CompletableFuture<Void>> futures = connectables.stream()
                .map(c -> c.connect().toCompletableFuture())
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    // Getters for internal components (useful for testing)

    public HeartbeatSupport getHeartbeat() {
        return heartbeat;
    }

    public ChannelSupport getChannels() {
        return channels;
    }

    public IntentSupport getIntents() {
        return intents;
    }

    public AppSupport getApps() {
        return apps;
    }
}

