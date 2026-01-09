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

package org.finos.fdc3.getagent;

import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.proxy.listeners.RegisterableListener;
import org.finos.fdc3.proxy.messaging.AbstractMessaging;
import org.finos.fdc3.proxy.util.Logger;

/**
 * WebSocket-based implementation of the Messaging interface.
 * <p>
 * This class manages the WebSocket connection to the FDC3 Desktop Agent
 * and handles sending/receiving messages.
 */
@ClientEndpoint
public class WebSocketMessaging extends AbstractMessaging {

    private final String webSocketUrl;
    private final Map<String, RegisterableListener> listeners = new ConcurrentHashMap<>();
    private Session session;
    private CompletableFuture<Void> connectionFuture;
    private volatile boolean connected = false;

    /**
     * Creates a new WebSocketMessaging instance.
     *
     * @param webSocketUrl  the WebSocket URL to connect to
     * @param appIdentifier the application identifier
     */
    public WebSocketMessaging(String webSocketUrl, AppIdentifier appIdentifier) {
        super(appIdentifier);
        this.webSocketUrl = webSocketUrl;
    }

    /**
     * Connects to the WebSocket server.
     *
     * @return a CompletionStage that completes when the connection is established
     */
    public CompletionStage<Void> connect() {
        if (connected && session != null && session.isOpen()) {
            return CompletableFuture.completedFuture(null);
        }

        connectionFuture = new CompletableFuture<>();

        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, URI.create(webSocketUrl));
        } catch (Exception e) {
            connectionFuture.completeExceptionally(e);
        }

        return connectionFuture;
    }

    @OnOpen
    public void onOpen(Session session) {
        Logger.info("WebSocket connection opened to {}", webSocketUrl);
        this.session = session;
        this.connected = true;
        if (connectionFuture != null) {
            connectionFuture.complete(null);
        }
    }

    @OnMessage
    public void onMessage(String message) {
        Logger.debug("Received message: {}", message);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> messageMap = getConverter().getObjectMapper().readValue(message, Map.class);
            
            // Dispatch to all registered listeners
            listeners.forEach((id, listener) -> {
                try {
                    if (listener.filter(messageMap)) {
                        listener.action(messageMap);
                    }
                } catch (Exception e) {
                    Logger.error("Error in listener {}: {}", id, e.getMessage());
                }
            });
        } catch (IOException e) {
            Logger.error("Failed to parse message: {}", e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        Logger.info("WebSocket connection closed: {}", closeReason.getReasonPhrase());
        this.connected = false;
        this.session = null;
    }

    @OnError
    public void onError(Session session, Throwable error) {
        Logger.error("WebSocket error: {}", error.getMessage());
        if (connectionFuture != null && !connectionFuture.isDone()) {
            connectionFuture.completeExceptionally(error);
        }
    }

    @Override
    public String createUUID() {
        return UUID.randomUUID().toString();
    }

    @Override
    public CompletionStage<Void> post(Map<String, Object> message) {
        if (session == null || !session.isOpen()) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("WebSocket is not connected"));
            return future;
        }

        try {
            String json = getConverter().toJson(message);
            Logger.debug("Sending message: {}", json);
            session.getAsyncRemote().sendText(json);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Override
    public void register(RegisterableListener listener) {
        if (listener.getId() == null) {
            throw new IllegalArgumentException("Listener must have ID set");
        }
        listeners.put(listener.getId(), listener);
    }

    @Override
    public void unregister(String id) {
        listeners.remove(id);
    }

    @Override
    public CompletionStage<Void> disconnect() {
        if (session == null || !session.isOpen()) {
            return CompletableFuture.completedFuture(null);
        }

        // Send WCP6Goodbye message before closing
        Map<String, Object> goodbye = new HashMap<>();
        goodbye.put("type", "WCP6Goodbye");
        Map<String, Object> meta = new HashMap<>();
        meta.put("timestamp", OffsetDateTime.now());
        goodbye.put("meta", meta);

        return post(goodbye).whenComplete((v, error) -> {
            // Close the WebSocket regardless of whether the goodbye was sent successfully
            try {
                session.close();
            } catch (IOException e) {
                Logger.error("Error closing WebSocket: {}", e.getMessage());
            }
            
            // Clear all listeners
            listeners.clear();
            connected = false;
        });
    }

    /**
     * Returns whether the WebSocket is currently connected.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return connected && session != null && session.isOpen();
    }
}
