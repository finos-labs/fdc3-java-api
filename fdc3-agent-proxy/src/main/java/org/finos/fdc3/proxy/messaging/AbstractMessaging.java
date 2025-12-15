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

package org.finos.fdc3.proxy.messaging;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.proxy.listeners.RegisterableListener;
import org.finos.fdc3.proxy.util.Logger;
import org.finos.fdc3.schema.AddContextListenerRequestMeta;
import org.finos.fdc3.schema.SchemaConverter;

/**
 * Abstract base class for messaging implementations.
 */
public abstract class AbstractMessaging implements Messaging {

    private static final String API_TIMEOUT = "ApiTimeout";
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final AppIdentifier appIdentifier;
    private final SchemaConverter converter;

    protected AbstractMessaging(AppIdentifier appIdentifier) {
        this.appIdentifier = appIdentifier;
        this.converter = new SchemaConverter();
    }

    @Override
    public abstract String createUUID();

    @Override
    public abstract CompletionStage<Void> post(Map<String, Object> message);

    @Override
    public abstract void register(RegisterableListener listener);

    @Override
    public abstract void unregister(String id);

    @Override
    public AddContextListenerRequestMeta createMeta() {
        AddContextListenerRequestMeta meta = new AddContextListenerRequestMeta();
        meta.setRequestUUID(createUUID());
        meta.setTimestamp(OffsetDateTime.now());

        if (appIdentifier != null) {
            org.finos.fdc3.schema.AppIdentifier source = new org.finos.fdc3.schema.AppIdentifier();
            source.setAppID(appIdentifier.getAppId());
            source.setDesktopAgent("testing-da");
            appIdentifier.getInstanceId().ifPresent(source::setInstanceID);
            meta.setSource(source);
        }
        return meta;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> CompletionStage<X> waitFor(Predicate<X> filter, long timeoutMs, String timeoutErrorMessage) {
        String id = createUUID();
        CompletableFuture<X> future = new CompletableFuture<>();

        final ScheduledFuture<?>[] timeoutFuture = new ScheduledFuture<?>[1];

        RegisterableListener listener = new RegisterableListener() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public boolean filter(Map<String, Object> message) {
                try {
                    return filter.test((X) message);
                } catch (ClassCastException e) {
                    return false;
                }
            }

            @Override
            public void action(Map<String, Object> message) {
                Logger.debug("Received from DesktopAgent: {}", message);
                unregister(id);
                if (timeoutFuture[0] != null) {
                    timeoutFuture[0].cancel(false);
                }
                future.complete((X) message);
            }

            @Override
            public CompletionStage<Void> register() {
                AbstractMessaging.this.register(this);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public void unsubscribe() {
                AbstractMessaging.this.unregister(id);
            }
        };

        register(listener);

        if (timeoutMs > 0) {
            timeoutFuture[0] = scheduler.schedule(() -> {
                unregister(id);
                if (!future.isDone()) {
                    Logger.error("waitFor rejecting after {}ms with {}", timeoutMs, timeoutErrorMessage);
                    future.completeExceptionally(new RuntimeException(timeoutErrorMessage));
                }
            }, timeoutMs, TimeUnit.MILLISECONDS);
        }

        return future;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> CompletionStage<X> exchange(Map<String, Object> message, String expectedTypeName, long timeoutMs) {
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        String requestUuid = (String) meta.get("requestUuid");

        CompletionStage<X> promise = waitFor(
                m -> {
                    Map<String, Object> msg = (Map<String, Object>) m;
                    String type = (String) msg.get("type");
                    Map<String, Object> msgMeta = (Map<String, Object>) msg.get("meta");
                    String respRequestUuid = msgMeta != null ? (String) msgMeta.get("requestUuid") : null;
                    return expectedTypeName.equals(type) && requestUuid.equals(respRequestUuid);
                },
                timeoutMs,
                API_TIMEOUT
        );

        Logger.debug("Sending to DesktopAgent: {}", message);
        post(message);

        return promise.thenApply(response -> {
            Map<String, Object> resp = (Map<String, Object>) response;
            Map<String, Object> payload = (Map<String, Object>) resp.get("payload");
            if (payload != null && payload.get("error") != null) {
                throw new RuntimeException((String) payload.get("error"));
            }
            return response;
        }).exceptionally(error -> {
            if (API_TIMEOUT.equals(error.getMessage())) {
                Logger.error("Timed-out while waiting for {} with requestUuid {}", expectedTypeName, requestUuid);
            }
            throw new RuntimeException(error);
        });
    }

    @Override
    public AppIdentifier getAppIdentifier() {
        return appIdentifier;
    }

    @Override
    public SchemaConverter getConverter() {
        return converter;
    }

    @Override
    public abstract CompletionStage<Void> disconnect();
}

