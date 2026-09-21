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

import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

import org.finos.fdc3.api.errors.FDC3ConnectionException;
import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.proxy.listeners.RegisterableListener;
import org.finos.fdc3.proxy.messaging.AbstractMessaging;
import org.finos.fdc3.proxy.util.Logger;
import org.finos.fdc3.proxy.util.MessageLogging;

/**
 * WebSocket-based implementation of the Messaging interface.
 * <p>
 * This class manages the WebSocket connection to the FDC3 Desktop Agent
 * and handles sending/receiving messages.
 */
@ClientEndpoint
public class WebSocketMessaging extends AbstractMessaging {

    /** Applied when no explicit connect timeout is supplied. */
    private static final long DEFAULT_CONNECT_TIMEOUT_MS = 10000;

    private final String webSocketUrl;
    private final long connectTimeoutMs;
    private final Map<String, RegisterableListener> listeners = new ConcurrentHashMap<>();

    // Assigned on the WebSocket container's callback threads and read by callers of post(),
    // disconnect() and isConnected(), so both need to be visible across threads.
    private volatile Session session;
    private volatile CompletableFuture<Void> connectionFuture;
    private volatile CompletableFuture<Void> disconnectFuture;
    private volatile boolean connected = false;

    /** Retained so its thread pools can be released on disconnect rather than leaked. */
    private volatile WebSocketContainer container;

    /** Set while {@link #disconnect()} is in progress, to tell a graceful close from a drop. */
    private volatile boolean closeExpected = false;

    /**
     * Creates a new WebSocketMessaging instance.
     *
     * @param webSocketUrl  the WebSocket URL to connect to
     * @param appIdentifier the application identifier
     */
    public WebSocketMessaging(String webSocketUrl, AppIdentifier appIdentifier) {
        this(webSocketUrl, appIdentifier, DEFAULT_CONNECT_TIMEOUT_MS);
    }

    /**
     * Creates a new WebSocketMessaging instance.
     *
     * @param webSocketUrl     the WebSocket URL to connect to
     * @param appIdentifier    the application identifier
     * @param connectTimeoutMs how long to wait for the WebSocket handshake to complete
     */
    public WebSocketMessaging(String webSocketUrl, AppIdentifier appIdentifier, long connectTimeoutMs) {
        super(appIdentifier);
        this.webSocketUrl = webSocketUrl;
        this.connectTimeoutMs = connectTimeoutMs;
    }

    /**
     * Connects to the WebSocket server.
     * <p>
     * The returned stage is bounded by the connect timeout. Without it, a server that accepts
     * the TCP connection but never completes the WebSocket upgrade would leave
     * {@code getAgent()} waiting indefinitely, since the message-level timeout only starts once
     * the connection is open.
     *
     * @return a CompletionStage that completes when the connection is established
     */
    public CompletionStage<Void> connect() {
        if (connected && session != null && session.isOpen()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> pending = new CompletableFuture<>();
        connectionFuture = pending;

        try {
            WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
            container = webSocketContainer;
            webSocketContainer.connectToServer(this, URI.create(webSocketUrl));
        } catch (Exception e) {
            pending.completeExceptionally(e);
        }

        return pending
                .orTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .exceptionally(error -> {
                    if (error instanceof TimeoutException
                            || error.getCause() instanceof TimeoutException) {
                        shutdownContainer();
                        throw new FDC3ConnectionException(
                                "Timed out after " + connectTimeoutMs
                                        + "ms waiting for the WebSocket connection to " + webSocketUrl);
                    }
                    if (error instanceof RuntimeException) {
                        throw (RuntimeException) error;
                    }
                    throw new FDC3ConnectionException("Failed to connect to " + webSocketUrl, error);
                });
    }

    @OnOpen
    public void onOpen(Session session) {
        Logger.info("WebSocket connection opened to {}", webSocketUrl);
        this.session = session;
        this.connected = true;
        CompletableFuture<Void> pending = connectionFuture;
        if (pending != null) {
            pending.complete(null);
        }
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> messageMap = getConverter().getObjectMapper().readValue(message, Map.class);

            Logger.debug("Received message: {}", MessageLogging.summarise(messageMap));
            if (Logger.isPayloadEnabled()) {
                Logger.payload("Received message: {}", MessageLogging.redact(messageMap));
            }


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
        this.connected = false;
        this.session = null;

        CompletableFuture<Void> pendingDisconnect = disconnectFuture;
        if (pendingDisconnect != null) {
            // We asked for this close, so it is the expected end of the WSCPGoodbye exchange.
            Logger.info("WebSocket connection closed: {}", closeReason.getReasonPhrase());
            pendingDisconnect.complete(null);
            return;
        }

        if (closeExpected) {
            Logger.info("WebSocket connection closed: {}", closeReason.getReasonPhrase());
            return;
        }

        // Nobody asked for this. Fail anything still waiting rather than letting callers find
        // out only when their next request times out.
        Logger.warn("WebSocket connection to {} was closed unexpectedly ({}: {}). "
                        + "Reconnecting is the application's responsibility.",
                webSocketUrl, closeReason.getCloseCode(), closeReason.getReasonPhrase());

        CompletableFuture<Void> pendingConnect = connectionFuture;
        if (pendingConnect != null && !pendingConnect.isDone()) {
            pendingConnect.completeExceptionally(new FDC3ConnectionException(
                    "WebSocket closed before the connection was established: "
                            + closeReason.getReasonPhrase()));
        }

        listeners.clear();
    }

    @OnError
    public void onError(Session session, Throwable error) {
        Logger.error("WebSocket error: {}", error.getMessage());
        CompletableFuture<Void> pending = connectionFuture;
        if (pending != null && !pending.isDone()) {
            pending.completeExceptionally(error);
        }
    }

    @Override
    public String createUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Logs an outbound message. The first message of every connection is
     * {@code WSCPApplicationConnect}, which carries {@code payload.sharedSecret}, so the body is
     * only ever written to the payload trace logger and only after redaction.
     */
    private static void logOutbound(Map<String, Object> message) {
        Logger.debug("Sending message: {}", MessageLogging.summarise(message));
        if (Logger.isPayloadEnabled()) {
            Logger.payload("Sending message: {}", MessageLogging.redact(message));
        }
    }

    @Override
    public CompletionStage<Void> post(Map<String, Object> message) {
        Session openSession = session;
        if (openSession == null || !openSession.isOpen()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("WebSocket is not connected"));
        }

        CompletableFuture<Void> sent = new CompletableFuture<>();
        try {
            String json = getConverter().toJson(message);
            logOutbound(message);

            // The send is asynchronous, so its outcome has to be reported through the handler.
            // Ignoring it made post() report success for sends that never happened, leaving
            // callers to wait out the full exchange timeout instead of failing immediately.
            openSession.getAsyncRemote().sendText(json, result -> {
                if (result.isOK()) {
                    sent.complete(null);
                } else {
                    sent.completeExceptionally(result.getException());
                }
            });
        } catch (Exception e) {
            sent.completeExceptionally(e);
        }
        return sent;
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
        closeExpected = true;

        Session openSession = session;
        if (openSession == null || !openSession.isOpen()) {
            listeners.clear();
            connected = false;
            releaseResources();
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> closed = new CompletableFuture<>();
        disconnectFuture = closed;

        // Send WSCPGoodbye and let the Desktop Agent close the socket (WSCP acceptor role).
        Map<String, Object> goodbye = new HashMap<>();
        goodbye.put("type", "WSCPGoodbye");
        Map<String, Object> meta = new HashMap<>();
        meta.put("timestamp", OffsetDateTime.now());
        goodbye.put("meta", meta);

        try {
            String json = getConverter().toJson(goodbye);
            logOutbound(goodbye);
            openSession.getBasicRemote().sendText(json);
        } catch (Exception e) {
            Logger.error("Failed to send WSCPGoodbye: {}", e.getMessage());
            try {
                openSession.close();
            } catch (IOException closeError) {
                Logger.error("Error closing WebSocket: {}", closeError.getMessage());
                closed.complete(null);
            }
        }

        return closed
                .orTimeout(5, TimeUnit.SECONDS)
                .handle((ignored, error) -> {
                    if (error != null) {
                        Logger.warn("Timed out waiting for Desktop Agent to close after WSCPGoodbye: {}",
                                error.getMessage());
                        try {
                            if (session != null && session.isOpen()) {
                                session.close();
                            }
                        } catch (IOException e) {
                            Logger.error("Error closing WebSocket after timeout: {}", e.getMessage());
                        }
                    }
                    listeners.clear();
                    connected = false;
                    disconnectFuture = null;
                    releaseResources();
                    return null;
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

    /** Releases the thread pools held by the container and the timeout scheduler. */
    private void releaseResources() {
        shutdownContainer();
        shutdownScheduler();
    }

    /**
     * Shuts down the retained container.
     * <p>
     * {@code WebSocketContainer} declares no lifecycle method, but implementations start thread
     * pools that outlive the connection. Tyrus exposes {@code shutdown()} on its container, so
     * it is called reflectively rather than compiling against a specific implementation.
     */
    private void shutdownContainer() {
        WebSocketContainer toShutDown = container;
        if (toShutDown == null) {
            return;
        }
        container = null;

        try {
            toShutDown.getClass().getMethod("shutdown").invoke(toShutDown);
        } catch (NoSuchMethodException e) {
            Logger.debug("WebSocket container {} has no shutdown() method; nothing to release",
                    toShutDown.getClass().getName());
        } catch (Exception e) {
            Logger.warn("Failed to shut down the WebSocket container: {}", e.getMessage());
        }
    }
}
