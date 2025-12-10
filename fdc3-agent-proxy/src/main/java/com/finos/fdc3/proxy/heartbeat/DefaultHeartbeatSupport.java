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

package com.finos.fdc3.proxy.heartbeat;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.finos.fdc3.proxy.Messaging;
import com.finos.fdc3.proxy.listeners.RegisterableListener;
import com.finos.fdc3.proxy.util.Logger;

/**
 * Default implementation of HeartbeatSupport.
 */
public class DefaultHeartbeatSupport implements HeartbeatSupport {

    private final Messaging messaging;
    private final long heartbeatIntervalMs;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> heartbeatTask;
    private RegisterableListener heartbeatListener;

    public DefaultHeartbeatSupport(Messaging messaging, long heartbeatIntervalMs) {
        this.messaging = messaging;
        this.heartbeatIntervalMs = heartbeatIntervalMs;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "fdc3-heartbeat");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Void> connect() {
        // Register listener for heartbeat events
        String id = messaging.createUUID();
        heartbeatListener = new RegisterableListener() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public boolean filter(Map<String, Object> message) {
                String type = (String) message.get("type");
                return "heartbeatEvent".equals(type);
            }

            @Override
            public void action(Map<String, Object> message) {
                Map<String, Object> payload = (Map<String, Object>) message.get("payload");
                String timestamp = (String) payload.get("timestamp");
                Logger.debug("Received heartbeat at {}", timestamp);
                
                // Respond to heartbeat
                respondToHeartbeat(message);
            }

            @Override
            public CompletionStage<Void> register() {
                messaging.register(this);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public void unsubscribe() {
                messaging.unregister(id);
            }
        };

        messaging.register(heartbeatListener);
        return CompletableFuture.completedFuture(null);
    }

    @SuppressWarnings("unchecked")
    private void respondToHeartbeat(Map<String, Object> heartbeatEvent) {
        try {
            Map<String, Object> meta = (Map<String, Object>) heartbeatEvent.get("meta");
            String requestUuid = (String) meta.get("requestUuid");

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            java.util.Map<String, Object> responseMeta = messaging.createMeta();
            responseMeta.put("requestUuid", requestUuid);
            response.put("meta", responseMeta);
            response.put("type", "heartbeatAcknowledgementRequest");

            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("heartbeatEventUuid", requestUuid);
            response.put("payload", payload);

            messaging.post(response);
        } catch (Exception e) {
            Logger.error("Failed to respond to heartbeat", e);
        }
    }

    @Override
    public CompletionStage<Void> disconnect() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(true);
        }
        if (heartbeatListener != null) {
            messaging.unregister(heartbeatListener.getId());
        }
        scheduler.shutdown();
        return CompletableFuture.completedFuture(null);
    }
}

