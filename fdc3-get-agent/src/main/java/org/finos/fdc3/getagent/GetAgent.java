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

package org.finos.fdc3.getagent;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.finos.fdc3.api.DesktopAgent;
import org.finos.fdc3.api.errors.FDC3ConnectionException;
import org.finos.fdc3.api.metadata.AppMetadata;
import org.finos.fdc3.api.metadata.ImplementationMetadata;
import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.api.ui.Connectable;
import org.finos.fdc3.proxy.DesktopAgentProxy;
import org.finos.fdc3.proxy.apps.DefaultAppSupport;
import org.finos.fdc3.proxy.channels.DefaultChannelSupport;
import org.finos.fdc3.proxy.heartbeat.DefaultHeartbeatSupport;
import org.finos.fdc3.proxy.intents.DefaultIntentSupport;
import org.finos.fdc3.proxy.util.Logger;
import org.finos.fdc3.schema.ProtocolVersion;
import org.finos.fdc3.schema.WebSocketConnectionProtocolApplicationConnect;
import org.finos.fdc3.schema.WebSocketConnectionProtocolApplicationConnectMeta;
import org.finos.fdc3.schema.WebSocketConnectionProtocolApplicationConnectPayload;
import org.finos.fdc3.schema.WebSocketConnectionProtocolApplicationConnectType;

/**
 * Factory for obtaining a DesktopAgent connection via WebSocket using WSCP.
 */
public class GetAgent {

    private static final String WSCP_DESKTOP_AGENT_CONNECT = "WSCPDesktopAgentConnect";
    private static final String WSCP_CONNECT_FAILED = "WSCPConnectFailed";

    private GetAgent() {
    }

    public static CompletionStage<DesktopAgent> getAgent(GetAgentParams params) {
        Logger.info("Initiating Desktop Agent connection to {}", params.getWebSocketUrl());

        AppIdentifier tempAppId = new AppIdentifier("pending", null, null);
        WebSocketMessaging messaging = new WebSocketMessaging(params.getWebSocketUrl(), tempAppId);

        return messaging.connect()
                .thenCompose(v -> performHandshake(messaging, params))
                .thenApply(validationResult -> createDesktopAgent(messaging, validationResult, params))
                .exceptionally(error -> {
                    try {
                        messaging.disconnect();
                    } catch (Exception e) {
                        Logger.error("Error during cleanup: {}", e.getMessage());
                    }

                    if (error.getCause() instanceof FDC3ConnectionException) {
                        throw (FDC3ConnectionException) error.getCause();
                    }
                    throw new FDC3ConnectionException("Failed to connect to Desktop Agent", error);
                });
    }

    private static CompletionStage<ValidationResult> performHandshake(
            WebSocketMessaging messaging, GetAgentParams params) {

        String connectionAttemptUuid = UUID.randomUUID().toString();

        WebSocketConnectionProtocolApplicationConnect connectMsg =
                new WebSocketConnectionProtocolApplicationConnect();
        connectMsg.setType(
                WebSocketConnectionProtocolApplicationConnectType.WSCP_APPLICATION_CONNECT);

        WebSocketConnectionProtocolApplicationConnectMeta meta =
                new WebSocketConnectionProtocolApplicationConnectMeta();
        meta.setConnectionAttemptUUID(connectionAttemptUuid);
        meta.setTimestamp(OffsetDateTime.now());
        connectMsg.setMeta(meta);

        WebSocketConnectionProtocolApplicationConnectPayload payload =
                new WebSocketConnectionProtocolApplicationConnectPayload();
        payload.setProtocolVersion(ProtocolVersion.THE_10);
        payload.setSharedSecret(params.getSharedSecret());
        connectMsg.setPayload(payload);

        Map<String, Object> connectMessage = messaging.getConverter().toMap(connectMsg);

        CompletableFuture<ValidationResult> responseFuture = new CompletableFuture<>();

        messaging.register(new org.finos.fdc3.proxy.listeners.RegisterableListener() {
            private final String id = UUID.randomUUID().toString();

            @Override
            public String getId() {
                return id;
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean filter(Map<String, Object> message) {
                String type = (String) message.get("type");
                if (!WSCP_DESKTOP_AGENT_CONNECT.equals(type)
                        && !WSCP_CONNECT_FAILED.equals(type)) {
                    return false;
                }

                Map<String, Object> msgMeta = (Map<String, Object>) message.get("meta");
                if (msgMeta == null) {
                    return false;
                }
                String respConnectionAttemptUuid = (String) msgMeta.get("connectionAttemptUuid");
                return connectionAttemptUuid.equals(respConnectionAttemptUuid);
            }

            @Override
            @SuppressWarnings("unchecked")
            public void action(Map<String, Object> message) {
                messaging.unregister(id);

                String type = (String) message.get("type");
                Map<String, Object> responsePayload = (Map<String, Object>) message.get("payload");

                if (WSCP_CONNECT_FAILED.equals(type)) {
                    String errorMessage = responsePayload != null
                            ? (String) responsePayload.get("message")
                            : "Connection failed";
                    responseFuture.completeExceptionally(
                            new FDC3ConnectionException("Connection failed: " + errorMessage));
                    return;
                }

                try {
                    ValidationResult result = new ValidationResult();

                    Map<String, Object> implMeta =
                            (Map<String, Object>) responsePayload.get("implementationMetadata");
                    if (implMeta != null) {
                        result.implementationMetadata = parseImplementationMetadata(implMeta);
                        AppMetadata appMetadata = result.implementationMetadata.getAppMetadata();
                        if (appMetadata != null) {
                            result.appId = appMetadata.getAppId();
                            result.instanceId = appMetadata.getInstanceId();
                        }
                    }

                    if (result.appId == null || result.instanceId == null) {
                        throw new FDC3ConnectionException(
                                "WSCPDesktopAgentConnect missing appMetadata appId/instanceId");
                    }

                    Logger.info("WSCP handshake successful - appId: {}, instanceId: {}",
                            result.appId, result.instanceId);
                    responseFuture.complete(result);
                } catch (Exception e) {
                    responseFuture.completeExceptionally(
                            new FDC3ConnectionException("Failed to parse WSCPDesktopAgentConnect", e));
                }
            }

            @Override
            public CompletionStage<Void> register() {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletionStage<Void> unsubscribe() {
                messaging.unregister(id);
                return CompletableFuture.completedFuture(null);
            }
        });

        Logger.debug("Sending WSCPApplicationConnect message");
        messaging.post(connectMessage);

        return responseFuture
                .orTimeout(params.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .exceptionally(error -> {
                    if (error instanceof TimeoutException
                            || (error.getCause() != null
                            && error.getCause() instanceof TimeoutException)) {
                        throw new FDC3ConnectionException(
                                "Connection timeout waiting for WSCPDesktopAgentConnect");
                    }
                    if (error instanceof FDC3ConnectionException) {
                        throw (FDC3ConnectionException) error;
                    }
                    throw new FDC3ConnectionException("Handshake failed", error);
                });
    }

    @SuppressWarnings("unchecked")
    private static ImplementationMetadata parseImplementationMetadata(Map<String, Object> implMeta) {
        ImplementationMetadata metadata = new ImplementationMetadata();

        metadata.setFdc3Version((String) implMeta.get("fdc3Version"));
        metadata.setProvider((String) implMeta.get("provider"));
        metadata.setProviderVersion((String) implMeta.get("providerVersion"));

        Map<String, Object> appMeta = (Map<String, Object>) implMeta.get("appMetadata");
        if (appMeta != null) {
            AppMetadata appMetadata = new AppMetadata();
            appMetadata.setAppId((String) appMeta.get("appId"));
            appMetadata.setInstanceId((String) appMeta.get("instanceId"));
            appMetadata.setName((String) appMeta.get("name"));
            appMetadata.setVersion((String) appMeta.get("version"));
            appMetadata.setTitle((String) appMeta.get("title"));
            appMetadata.setTooltip((String) appMeta.get("tooltip"));
            appMetadata.setDescription((String) appMeta.get("description"));
            metadata.setAppMetadata(appMetadata);
        }

        Map<String, Object> optFeatures = (Map<String, Object>) implMeta.get("optionalFeatures");
        if (optFeatures != null) {
            ImplementationMetadata.OptionalFeatures features =
                    new ImplementationMetadata.OptionalFeatures();
            if (optFeatures.get("OriginatingAppMetadata") != null) {
                features.setOriginatingAppMetadata((Boolean) optFeatures.get("OriginatingAppMetadata"));
            }
            if (optFeatures.get("UserChannelMembershipAPIs") != null) {
                features.setUserChannelMembershipAPIs(
                        (Boolean) optFeatures.get("UserChannelMembershipAPIs"));
            }
            if (optFeatures.get("DesktopAgentBridging") != null) {
                features.setDesktopAgentBridging((Boolean) optFeatures.get("DesktopAgentBridging"));
            }
            metadata.setOptionalFeatures(features);
        }

        return metadata;
    }

    private static DesktopAgent createDesktopAgent(
            WebSocketMessaging messaging,
            ValidationResult validationResult,
            GetAgentParams params) {

        AppIdentifier appIdentifier = new AppIdentifier(
                validationResult.appId,
                validationResult.instanceId,
                null);

        messaging.setIdentifier(appIdentifier, validationResult.instanceId);

        DefaultHeartbeatSupport heartbeatSupport = new DefaultHeartbeatSupport(
                messaging, params.getHeartbeatIntervalMs());

        DefaultChannelSupport channelSupport = new DefaultChannelSupport(
                messaging, params.getChannelSelector(), params.getMessageExchangeTimeout());

        DefaultIntentSupport intentSupport = new DefaultIntentSupport(
                messaging, params.getIntentResolver(),
                params.getMessageExchangeTimeout(), params.getAppLaunchTimeout());

        DefaultAppSupport appSupport = new DefaultAppSupport(
                messaging, params.getMessageExchangeTimeout(), params.getAppLaunchTimeout());

        List<Connectable> connectables = new ArrayList<>();
        connectables.add(heartbeatSupport);

        DesktopAgentProxy proxy = new DesktopAgentProxy(
                heartbeatSupport,
                channelSupport,
                intentSupport,
                appSupport,
                connectables);

        proxy.connect();

        Logger.info("DesktopAgent proxy created successfully");
        return proxy;
    }

    private static class ValidationResult {
        String appId;
        String instanceId;
        ImplementationMetadata implementationMetadata;
    }
}
