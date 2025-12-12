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

package com.finos.fdc3.adapter.openfin;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finos.fdc3.api.DesktopAgent;
import com.finos.fdc3.api.channel.Channel;
import com.finos.fdc3.api.channel.Channel.Type;
import com.finos.fdc3.api.channel.PrivateChannel;
import com.finos.fdc3.api.context.Context;
import com.finos.fdc3.api.errors.FDC3ConnectionException;
import com.finos.fdc3.api.metadata.AppIntent;
import com.finos.fdc3.api.metadata.AppMetadata;
import com.finos.fdc3.api.metadata.ImplementationMetadata;
import com.finos.fdc3.api.metadata.IntentResolution;
import com.finos.fdc3.api.types.AppIdentifier;
import com.finos.fdc3.api.types.ContextHandler;
import com.finos.fdc3.api.types.IntentHandler;
import com.finos.fdc3.api.types.Listener;
import com.openfin.desktop.DesktopConnection;
import com.openfin.desktop.DesktopStateListener;
import com.openfin.desktop.interop.Intent;
import com.openfin.desktop.interop.InteropClient;

/**
 * FDC3 {@link DesktopAgent} implementation using OpenFin Java adapter
 * 
 * @author Tim Jenkel
 */
public class OpenFinDesktopAgent implements DesktopAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenFinDesktopAgent.class);

    private final OpenFinFDC3Config config;

    private DesktopConnection desktopConnection;

    private CompletableFuture<InteropClient> interopClientFuture;

    private volatile String currentChannelId;

    public OpenFinDesktopAgent(OpenFinFDC3Config config) {

        this.config = config;
    }

    protected OpenFinFDC3Config getConfig() {

        return config;
    }

    protected CompletionStage<InteropClient> getInteropClient() {

        if (interopClientFuture == null || interopClientFuture.isCompletedExceptionally()) {
            interopClientFuture = new CompletableFuture<InteropClient>().orTimeout(config.getTimeout().toMillis(),
                TimeUnit.MILLISECONDS);
            CompletableFuture.runAsync(this::connect);
        }
        return interopClientFuture;
    }

    private void connect() {

        try {
            if (desktopConnection == null) {
                LOGGER.info("Initializing OpenFin connection");
                desktopConnection = new DesktopConnection(getConfig().getUuid());
            }
            if (desktopConnection.isConnected()) {
                createInteropClient();
            } else {
                LOGGER.info("Connecting to OpenFin");
                desktopConnection.connect(getConfig().getRuntimeConfiguration(), new OpenFinStateListener(),
                    (int) getConfig().getTimeout().toSeconds());
            }
        } catch (Exception e) {
            String msg = "Exception connecting to OpenFin";
            LOGGER.error(msg, e);
            Optional.ofNullable(interopClientFuture)
                .ifPresent(future -> future.completeExceptionally(new FDC3ConnectionException(msg, e)));
        }
    }

    private void createInteropClient() {

        LOGGER.info("Creating interop client");
        desktopConnection.getInterop().connect(getConfig().getBrokerName()).thenAccept(client -> {
            LOGGER.info("Connected interop client");
            if (interopClientFuture == null) {
                interopClientFuture = CompletableFuture.completedFuture(client);
            } else {
                interopClientFuture.complete(client);
            }
        });
    }

    @Override
    public CompletionStage<AppIdentifier> open(AppIdentifier app, Context context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<AppIntent> findIntent(String indent, Context context, String resultType) {

        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<List<AppIntent>> findIntentsByContext(Context context, String resultType) {

        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<List<AppIdentifier>> findInstances(AppIdentifier app) {

        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Void> broadcast(Context context) {

        return getInteropClient().thenCompose(client -> client.setContext(convertToOpenFinContext(context)));
    }
    
    protected CompletionStage<Void> broadcastOnChannel(Context context, String channelId) {

        return getInteropClient().thenCompose(client -> {
            if (channelId.equals(currentChannelId)) {
                return client.setContext(convertToOpenFinContext(context));
            }
            throw new UnsupportedOperationException("Broadcasting is only supported on the currently joined channel");
        });
    }

    @Override
    public CompletionStage<IntentResolution> raiseIntent(String intentName, Context context) {

        Intent intent = new Intent();
        intent.setName(intentName);
        intent.setContext(convertToOpenFinContext(context));
        return getInteropClient().thenCompose(client -> client.fireIntent(intent)).thenApply(result -> {
            // TODO OpenFin doesn't provide IntentResolution details
            return null;
        });
    }

    @Override
    public CompletionStage<IntentResolution> raiseIntent(String intentName, Context context, AppIdentifier app) {

        if (app == null) {
            return raiseIntent(intentName, context);
        }
        throw new UnsupportedOperationException("raiseIntent for a specific app is not supported");
    }

    @Override
    public CompletionStage<IntentResolution> raiseIntentForContext(Context context, AppIdentifier app) {

        // TODO confirm if OpenFin handling of intent without name matches FDC3 spec for raiseIntentForContext
        return raiseIntent(null, context, app);
    }

    @Override
    public CompletionStage<Listener> addContextListener(String contextType, ContextHandler handler) {

        final OpenFinContextListenerAdapter listener = new OpenFinContextListenerAdapter(handler);
        return getInteropClient().thenCompose(client -> client.addContextListener(contextType, listener))
            .thenApply(result -> (() -> getInteropClient()
                .thenCompose(client -> client.removeContextListener(contextType, listener)
                    .thenRun(() -> LOGGER.info("Unsubscribed context handler for type {}", contextType))
                    .exceptionally(ex -> {
                        LOGGER.error("Error unsubscribing context handler for type {}", contextType, ex);
                        return null;
                    }))));
    }
    
    @Override
    public CompletionStage<Listener> addIntentListener(String intent, IntentHandler handler) {

        final OpenFinIntentListenerAdapter listener = new OpenFinIntentListenerAdapter(handler);
        return getInteropClient().thenCompose(client -> client.registerIntentListener(intent, listener))
            .thenApply(result -> (() -> {
                throw new UnsupportedOperationException("Removing an intent listener is not supported");
            }));
    };

    @Override
    public CompletionStage<Void> joinUserChannel(String channelId) {

        return getInteropClient().thenCompose(client -> client.joinContextGroup(channelId))
            .thenRun(() -> currentChannelId = channelId);
    }

    @Override
    public CompletionStage<List<Channel>> getUserChannels() {

        return getInteropClient().thenCompose(InteropClient::getContextGroups).thenApply(groups -> Stream.of(groups)
            .map(group -> new OpenFinChannel(group, Type.User, this)).collect(Collectors.toList()));
    }

    @Override
    public CompletionStage<Channel> getOrCreateChannel(String channelId) {

        // TODO this will get the channel if it exists, but will throw an error if the channel doesn't exist
        // need OpenFin support to create App channels
        return getInteropClient().thenCompose(client -> client.getInfoForContextGroup(channelId))
            .thenApply(group -> new OpenFinChannel(group, Type.User, this));
    }
    
    @Override
    public CompletionStage<PrivateChannel> createPrivateChannel() {
    
        throw new UnsupportedOperationException();
    }
    
    @Override
    public CompletionStage<Optional<Channel>> getCurrentChannel() {

        return Optional.ofNullable(currentChannelId)
            .map(channelId -> getOrCreateChannel(channelId).thenApply(Optional::of))
            .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()));
    }

    @Override
    public CompletionStage<Void> leaveCurrentChannel() {

        return getInteropClient().thenCompose(InteropClient::removeFromContextGroup)
            .thenRun(() -> currentChannelId = null);
    }

    @Override
    public CompletionStage<ImplementationMetadata> getInfo() {

        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletionStage<AppMetadata> getAppMetadata(AppIdentifier app) {

        throw new UnsupportedOperationException();
    }

    protected com.openfin.desktop.interop.Context convertToOpenFinContext(Context context) {

        return new com.openfin.desktop.interop.Context(new JSONObject(context));
    }

    private class OpenFinStateListener implements DesktopStateListener {

        @Override
        public void onReady() {

            LOGGER.info("OpenFin connection ready");
            createInteropClient();
        }

        @Override
        public void onClose(String error) {

            LOGGER.warn("OpenFin connection closed: {}", error);
        }

        @Override
        public void onError(String reason) {

            String msg = "Error connecting to OpenFin: " + reason;
            LOGGER.error(msg);
            Optional.ofNullable(interopClientFuture)
                .ifPresent(future -> interopClientFuture.completeExceptionally(new FDC3ConnectionException(msg)));
        }

        @Override
        public void onMessage(String message) {

            LOGGER.debug("OpenFin message in: {}", message);
        }

        @Override
        public void onOutgoingMessage(String message) {

            LOGGER.debug("OpenFin message out: {}", message);
        }

    }

}
