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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.finos.fdc3.api.DesktopAgent;
import org.finos.fdc3.api.errors.FDC3ConnectionException;
import org.finos.fdc3.api.metadata.ImplementationMetadata;
import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.api.ui.Connectable;
import org.finos.fdc3.proxy.DesktopAgentProxy;
import org.finos.fdc3.proxy.apps.DefaultAppSupport;
import org.finos.fdc3.proxy.channels.DefaultChannelSupport;
import org.finos.fdc3.proxy.heartbeat.DefaultHeartbeatSupport;
import org.finos.fdc3.proxy.intents.DefaultIntentSupport;
import org.finos.fdc3.proxy.util.Logger;

/**
 * Factory for obtaining a DesktopAgent connection via WebSocket.
 * <p>
 * This class handles the Web Connection Protocol (WCP) handshake to establish
 * a connection to an FDC3 Desktop Agent over WebSocket.
 * <p>
 * Usage example:
 * <pre>{@code
 * GetAgentParams params = GetAgentParams.builder()
 *     .webSocketUrl("ws://localhost:8080/fdc3")
 *     .identityUrl("https://myapp.example.com/")
 *     .channelSelector(myChannelSelector)
 *     .intentResolver(myIntentResolver)
 *     .build();
 *
 * DesktopAgent agent = GetAgent.getAgent(params).toCompletableFuture().get();
 * }</pre>
 */
public class GetAgent {

    private static final String WCP4_VALIDATE_APP_IDENTITY = "WCP4ValidateAppIdentity";
    private static final String WCP5_VALIDATE_APP_IDENTITY_RESPONSE = "WCP5ValidateAppIdentityResponse";
    private static final String WCP5_VALIDATE_APP_IDENTITY_FAILED_RESPONSE = "WCP5ValidateAppIdentityFailedResponse";

    private GetAgent() {
        // Utility class - no instantiation
    }

    /**
     * Obtains a DesktopAgent connection using the provided parameters.
     * <p>
     * This method:
     * <ol>
     *   <li>Establishes a WebSocket connection to the Desktop Agent</li>
     *   <li>Sends a WCP4ValidateAppIdentity message</li>
     *   <li>Waits for WCP5ValidateAppIdentityResponse or WCP5ValidateAppIdentityFailedResponse</li>
     *   <li>If successful, constructs and returns a DesktopAgentProxy</li>
     * </ol>
     *
     * @param params the connection parameters
     * @return a CompletionStage that completes with the DesktopAgent, or fails with an exception
     * @throws FDC3ConnectionException if the connection fails
     */
    public static CompletionStage<DesktopAgent> getAgent(GetAgentParams params) {
        Logger.info("Initiating Desktop Agent connection to {}", params.getWebSocketUrl());

        // Create a temporary AppIdentifier for the initial connection
        // This will be updated once we receive the identity validation response
        AppIdentifier tempAppId = new AppIdentifier("pending", null, null);
        WebSocketMessaging messaging = new WebSocketMessaging(params.getWebSocketUrl(), tempAppId);

        return messaging.connect()
                .thenCompose(v -> performHandshake(messaging, params))
                .thenApply(validationResult -> createDesktopAgent(messaging, validationResult, params))
                .exceptionally(error -> {
                    // Clean up on failure
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

    /**
     * Performs the WCP4/WCP5 handshake with the Desktop Agent.
     */
    private static CompletionStage<ValidationResult> performHandshake(
            WebSocketMessaging messaging, GetAgentParams params) {
        
        String connectionAttemptUuid = UUID.randomUUID().toString();

        // Build WCP4ValidateAppIdentity message
        Map<String, Object> validateMessage = new HashMap<>();
        validateMessage.put("type", WCP4_VALIDATE_APP_IDENTITY);

        Map<String, Object> meta = new HashMap<>();
        meta.put("connectionAttemptUuid", connectionAttemptUuid);
        meta.put("timestamp", OffsetDateTime.now().toString());
        validateMessage.put("meta", meta);

        Map<String, Object> payload = new HashMap<>();
        payload.put("identityUrl", params.getIdentityUrl());
        payload.put("actualUrl", params.getIdentityUrl()); // In Java, these are typically the same

        // Include instanceId and instanceUuid if provided (for reconnection)
        if (params.getInstanceId() != null) {
            payload.put("instanceId", params.getInstanceId());
        }
        if (params.getInstanceUuid() != null) {
            payload.put("instanceUuid", params.getInstanceUuid());
        }
        validateMessage.put("payload", payload);

        // Set up response listener
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
                if (!WCP5_VALIDATE_APP_IDENTITY_RESPONSE.equals(type) &&
                    !WCP5_VALIDATE_APP_IDENTITY_FAILED_RESPONSE.equals(type)) {
                    return false;
                }

                // Verify the connectionAttemptUuid matches
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

                if (WCP5_VALIDATE_APP_IDENTITY_FAILED_RESPONSE.equals(type)) {
                    String errorMessage = responsePayload != null 
                            ? (String) responsePayload.get("message") 
                            : "Identity validation failed";
                    responseFuture.completeExceptionally(
                            new FDC3ConnectionException("Identity validation failed: " + errorMessage));
                    return;
                }

                // Parse successful response
                try {
                    ValidationResult result = new ValidationResult();
                    result.appId = (String) responsePayload.get("appId");
                    result.instanceId = (String) responsePayload.get("instanceId");
                    result.instanceUuid = (String) responsePayload.get("instanceUuid");

                    // Parse implementation metadata
                    Map<String, Object> implMeta = (Map<String, Object>) responsePayload.get("implementationMetadata");
                    if (implMeta != null) {
                        result.implementationMetadata = parseImplementationMetadata(implMeta);
                    }

                    Logger.info("Identity validation successful - appId: {}, instanceId: {}",
                            result.appId, result.instanceId);
                    responseFuture.complete(result);
                } catch (Exception e) {
                    responseFuture.completeExceptionally(
                            new FDC3ConnectionException("Failed to parse validation response", e));
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

        // Send the validation message
        Logger.debug("Sending WCP4ValidateAppIdentity message");
        messaging.post(validateMessage);

        // Apply timeout
        return responseFuture
                .orTimeout(params.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .exceptionally(error -> {
                    if (error instanceof TimeoutException || 
                        (error.getCause() != null && error.getCause() instanceof TimeoutException)) {
                        throw new FDC3ConnectionException("Connection timeout waiting for identity validation");
                    }
                    if (error instanceof FDC3ConnectionException) {
                        throw (FDC3ConnectionException) error;
                    }
                    throw new FDC3ConnectionException("Handshake failed", error);
                });
    }

    /**
     * Parses the implementation metadata from the validation response.
     */
    @SuppressWarnings("unchecked")
    private static ImplementationMetadata parseImplementationMetadata(Map<String, Object> implMeta) {
        ImplementationMetadata metadata = new ImplementationMetadata();
        
        metadata.setFdc3Version((String) implMeta.get("fdc3Version"));
        metadata.setProvider((String) implMeta.get("provider"));
        metadata.setProviderVersion((String) implMeta.get("providerVersion"));

        // Parse app metadata
        Map<String, Object> appMeta = (Map<String, Object>) implMeta.get("appMetadata");
        if (appMeta != null) {
            org.finos.fdc3.api.metadata.AppMetadata appMetadata = new org.finos.fdc3.api.metadata.AppMetadata();
            appMetadata.setAppId((String) appMeta.get("appId"));
            appMetadata.setInstanceId((String) appMeta.get("instanceId"));
            appMetadata.setName((String) appMeta.get("name"));
            appMetadata.setVersion((String) appMeta.get("version"));
            appMetadata.setTitle((String) appMeta.get("title"));
            appMetadata.setTooltip((String) appMeta.get("tooltip"));
            appMetadata.setDescription((String) appMeta.get("description"));
            metadata.setAppMetadata(appMetadata);
        }

        // Parse optional features
        Map<String, Object> optFeatures = (Map<String, Object>) implMeta.get("optionalFeatures");
        if (optFeatures != null) {
            ImplementationMetadata.OptionalFeatures features = new ImplementationMetadata.OptionalFeatures();
            if (optFeatures.get("OriginatingAppMetadata") != null) {
                features.setOriginatingAppMetadata((Boolean) optFeatures.get("OriginatingAppMetadata"));
            }
            if (optFeatures.get("UserChannelMembershipAPIs") != null) {
                features.setUserChannelMembershipAPIs((Boolean) optFeatures.get("UserChannelMembershipAPIs"));
            }
            if (optFeatures.get("DesktopAgentBridging") != null) {
                features.setDesktopAgentBridging((Boolean) optFeatures.get("DesktopAgentBridging"));
            }
            metadata.setOptionalFeatures(features);
        }

        return metadata;
    }

    /**
     * Creates the DesktopAgentProxy with all support components.
     */
    private static DesktopAgent createDesktopAgent(
            WebSocketMessaging messaging,
            ValidationResult validationResult,
            GetAgentParams params) {

        // Create the final AppIdentifier with the validated identity
        AppIdentifier appIdentifier = new AppIdentifier(
                validationResult.appId,
                validationResult.instanceId,
                null // desktopAgent is not set on the app's own identifier
        );

        // Create a Messaging wrapper with the correct AppIdentifier
        org.finos.fdc3.proxy.Messaging finalMessaging = new WebSocketMessagingWrapper(messaging, appIdentifier);

        // Create support components
        DefaultHeartbeatSupport heartbeatSupport = new DefaultHeartbeatSupport(
                finalMessaging, params.getHeartbeatIntervalMs());

        DefaultChannelSupport channelSupport = new DefaultChannelSupport(
                finalMessaging, params.getChannelSelector(), params.getMessageExchangeTimeout());

        DefaultIntentSupport intentSupport = new DefaultIntentSupport(
                finalMessaging, params.getIntentResolver(),
                params.getMessageExchangeTimeout(), params.getAppLaunchTimeout());

        DefaultAppSupport appSupport = new DefaultAppSupport(
                finalMessaging, params.getMessageExchangeTimeout(), params.getAppLaunchTimeout());

        // Build list of connectables (for connect/disconnect lifecycle)
        List<Connectable> connectables = new ArrayList<>();
        connectables.add(heartbeatSupport);

        // Create and return the DesktopAgentProxy
        DesktopAgentProxy proxy = new DesktopAgentProxy(
                heartbeatSupport,
                channelSupport,
                intentSupport,
                appSupport,
                connectables);

        // Start the heartbeat and other connectables
        proxy.connect();

        Logger.info("DesktopAgent proxy created successfully");
        return proxy;
    }

    /**
     * Holds the result of a successful identity validation.
     */
    private static class ValidationResult {
        String appId;
        String instanceId;
        String instanceUuid;
        ImplementationMetadata implementationMetadata;
    }

    /**
     * Wrapper around WebSocketMessaging to provide the correct AppIdentifier
     * after identity validation while delegating all other operations.
     */
    private static class WebSocketMessagingWrapper implements org.finos.fdc3.proxy.Messaging {
        private final WebSocketMessaging delegate;
        private final AppIdentifier appIdentifier;

        WebSocketMessagingWrapper(WebSocketMessaging delegate, AppIdentifier appIdentifier) {
            this.delegate = delegate;
            this.appIdentifier = appIdentifier;
        }

        @Override
        public String createUUID() {
            return delegate.createUUID();
        }

        @Override
        public CompletionStage<Void> post(Map<String, Object> message) {
            return delegate.post(message);
        }

        @Override
        public void register(org.finos.fdc3.proxy.listeners.RegisterableListener listener) {
            delegate.register(listener);
        }

        @Override
        public void unregister(String id) {
            delegate.unregister(id);
        }

        @Override
        public org.finos.fdc3.schema.AddContextListenerRequestMeta createMeta() {
            // Create meta with the correct AppIdentifier
            org.finos.fdc3.schema.AddContextListenerRequestMeta meta = 
                    new org.finos.fdc3.schema.AddContextListenerRequestMeta();
            meta.setRequestUUID(createUUID());
            meta.setTimestamp(OffsetDateTime.now());
            meta.setSource(appIdentifier);
            return meta;
        }

        @Override
        public <X> CompletionStage<X> waitFor(
                java.util.function.Predicate<X> filter, long timeoutMs, String timeoutErrorMessage) {
            return delegate.waitFor(filter, timeoutMs, timeoutErrorMessage);
        }

        @Override
        public <X> CompletionStage<X> exchange(
                Map<String, Object> message, String expectedTypeName, long timeoutMs) {
            return delegate.exchange(message, expectedTypeName, timeoutMs);
        }

        @Override
        public AppIdentifier getAppIdentifier() {
            return appIdentifier;
        }

        @Override
        public CompletionStage<Void> disconnect() {
            return delegate.disconnect();
        }

        @Override
        public org.finos.fdc3.schema.SchemaConverter getConverter() {
            return delegate.getConverter();
        }
    }
}
