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

package org.finos.fdc3.workbench.connection;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.finos.fdc3.api.DesktopAgent;
import org.finos.fdc3.getagent.GetAgent;
import org.finos.fdc3.getagent.GetAgentParams;
import org.finos.fdc3.proxy.DesktopAgentProxy;
import org.finos.fdc3.workbench.service.AgentHolder;
import org.finos.fdc3.workbench.service.SystemLogService;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * WSCP connect / disconnect / reconnect, matching fdc3-example-app semantics.
 */
public class ConnectionService {

    private final AgentHolder agentHolder;
    private final SystemLogService log;
    private final BooleanProperty connecting = new SimpleBooleanProperty(false);

    private String webSocketUrl;
    private String sharedSecret;
    private Consumer<DesktopAgent> onConnected = a -> {
    };
    private Consumer<Void> onDisconnected = v -> {
    };

    private final AtomicReference<String> pendingOpenUri = new AtomicReference<>();

    public ConnectionService(AgentHolder agentHolder, SystemLogService log) {
        this.agentHolder = agentHolder;
        this.log = log;
    }

    public BooleanProperty connectingProperty() {
        return connecting;
    }

    public String getWebSocketUrl() {
        return webSocketUrl;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public boolean hasCredentials() {
        return webSocketUrl != null && sharedSecret != null;
    }

    public void setOnConnected(Consumer<DesktopAgent> onConnected) {
        this.onConnected = onConnected != null ? onConnected : a -> {
        };
    }

    public void setOnDisconnected(Consumer<Void> onDisconnected) {
        this.onDisconnected = onDisconnected != null ? onDisconnected : v -> {
        };
    }

    public void setCredentials(String webSocketUrl, String sharedSecret) {
        this.webSocketUrl = webSocketUrl;
        this.sharedSecret = sharedSecret;
    }

    public void setPendingOpenUri(String uri) {
        pendingOpenUri.set(uri);
    }

    public String takePendingOpenUri() {
        return pendingOpenUri.getAndSet(null);
    }

    public void applyProtocolLaunch(ProtocolLaunchParams params, String source) {
        setCredentials(params.getWebSocketUrl(), params.getSharedSecret());
        log.info("connection", "Using WSCP connection config from " + source);
        if (agentHolder.isConnected()) {
            disconnect();
        }
        connect();
    }

    public boolean applyProtocolUri(String uri, String source) {
        var parsed = ProtocolLaunchParams.parseLaunchUri(uri);
        if (parsed.isEmpty()) {
            log.error("connection", "Could not parse protocol launch URL from " + source);
            return false;
        }
        applyProtocolLaunch(parsed.get(), source);
        return true;
    }

    public void connect() {
        if (!hasCredentials()) {
            log.error("connection", "WSCP connection config incomplete");
            return;
        }
        if (connecting.get()) {
            return;
        }
        connecting.set(true);
        agentHolder.setStatus("Connecting...", "status-disconnected");
        log.info("connection", "Connecting to Desktop Agent at: " + webSocketUrl);

        try {
            GetAgentParams params = GetAgentParams.builder()
                    .timeoutMs(30000)
                    .webSocketUrl(webSocketUrl)
                    .sharedSecret(sharedSecret)
                    .build();

            GetAgent.getAgent(params)
                    .thenAccept(this::handleConnected)
                    .exceptionally(error -> {
                        Platform.runLater(() -> handleConnectionError(error));
                        return null;
                    });
        } catch (Exception e) {
            handleConnectionError(e);
        }
    }

    public void reconnect() {
        if (!hasCredentials()) {
            log.error("connection", "webSocketUrl and sharedSecret required for reconnection");
            return;
        }
        log.info("connection", "Reconnecting to Desktop Agent...");
        agentHolder.setStatus("Reconnecting...", "status-disconnected");
        connect();
    }

    public void disconnect() {
        DesktopAgent agent = agentHolder.agentProperty().get();
        if (agent == null) {
            return;
        }
        log.info("connection", "Disconnecting from Desktop Agent...");
        if (agent instanceof DesktopAgentProxy proxy) {
            try {
                proxy.disconnect().toCompletableFuture().join();
            } catch (Exception e) {
                log.error("connection", "Error during disconnect - " + e.getMessage());
            }
        }
        agentHolder.setAgent(null);
        agentHolder.setInfo(null);
        agentHolder.setStatus("Disconnected", "status-disconnected");
        log.info("connection", "Disconnected from Desktop Agent");
        onDisconnected.accept(null);
    }

    private void handleConnected(DesktopAgent agent) {
        Platform.runLater(() -> {
            connecting.set(false);
            agentHolder.setAgent(agent);
            agentHolder.setStatus("Connected", "status-connected");
            log.info("connection", "Successfully connected to Desktop Agent");
            agent.getInfo()
                    .thenAccept(info -> Platform.runLater(() -> {
                        agentHolder.setInfo(info);
                        if (info != null && info.getAppMetadata() != null
                                && info.getAppMetadata().getInstanceId() != null) {
                            log.info("connection",
                                    "Assigned instanceId: " + info.getAppMetadata().getInstanceId());
                        }
                    }))
                    .exceptionally(error -> {
                        log.error("connection", "Could not retrieve getInfo() - " + error.getMessage());
                        return null;
                    });
            onConnected.accept(agent);
        });
    }

    private void handleConnectionError(Throwable error) {
        connecting.set(false);
        agentHolder.setStatus("Connection Failed", "status-error");
        log.error("connection", "Failed to connect - " + error.getMessage());
    }

    public static String envOrProperty(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = System.getProperty(key);
        }
        return value == null || value.isBlank() ? null : value.trim();
    }
}
