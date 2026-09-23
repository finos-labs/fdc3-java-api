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

package org.finos.fdc3.proxy.messaging;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
import org.finos.fdc3.proxy.util.MessageLogging;
import org.finos.fdc3.schema.AddContextListenerRequestMeta;
import org.finos.fdc3.schema.SchemaConverter;

/**
 * Abstract base class for messaging implementations.
 */
public abstract class AbstractMessaging implements Messaging {

    private static final String API_TIMEOUT = "ApiTimeout";

    /**
     * Schedules the timeouts for {@link #waitFor}. Per instance and daemon so that an
     * application which returns from {@code main} without calling {@link #disconnect()} still
     * exits, and so that {@link #shutdownScheduler()} cannot affect another connection.
     */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "fdc3-messaging-timeouts");
        thread.setDaemon(true);
        return thread;
    });

    private AppIdentifier appIdentifier;
    private final SchemaConverter converter;

    protected AbstractMessaging(AppIdentifier appIdentifier) {
        this.appIdentifier = appIdentifier;
        this.converter = new SchemaConverter();
    }

    /**
     * Stops the timeout scheduler. Subclasses must call this from {@link #disconnect()} so the
     * thread does not outlive the connection.
     */
    protected void shutdownScheduler() {
        scheduler.shutdownNow();
    }

    /**
     * Sets the identity for this messaging instance after validation.
     * This is called after the handshake when the Desktop Agent provides
     * the validated AppIdentifier.
     *
     * @param appIdentifier the validated app identifier
     */
    public void setIdentifier(AppIdentifier appIdentifier) {
        this.appIdentifier = appIdentifier;
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
            // Copied rather than shared so that a later setIdentifier cannot alter the source of
            // a message already in flight. The desktopAgent field is carried through as the
            // Desktop Agent supplied it during the handshake; inventing a value here would have
            // every message misreport which agent it came from.
            meta.setSource(new AppIdentifier(
                    appIdentifier.getAppId(),
                    appIdentifier.getInstanceId(),
                    appIdentifier.getDesktopAgent()));
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
                Logger.debug("Received from DesktopAgent: {}", MessageLogging.summarise(message));
                if (Logger.isPayloadEnabled()) {
                    Logger.payload("Received from DesktopAgent: {}", MessageLogging.redact(message));
                }
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
            public CompletionStage<Void> unsubscribe() {
                AbstractMessaging.this.unregister(id);
                return CompletableFuture.completedFuture(null);
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

        Logger.debug("Sending to DesktopAgent: {}", MessageLogging.summarise(message));
        if (Logger.isPayloadEnabled()) {
            Logger.payload("Sending to DesktopAgent: {}", MessageLogging.redact(message));
        }


        // Wait for post to complete before proceeding to ensure message is recorded
        return post(message).thenCompose(v -> promise).thenApply(response -> {
            Map<String, Object> resp = (Map<String, Object>) response;
            Map<String, Object> payload = (Map<String, Object>) resp.get("payload");
            if (payload != null && payload.get("error") != null) {
                throw new RuntimeException((String) payload.get("error"));
            }
            return response;
        }).exceptionally(error -> {
            // The stage hands us a CompletionException wrapping the real failure. Rethrowing it
            // as-is would add a second wrapper, leaving callers to unwrap twice to reach the
            // FDC3 error name they are supposed to match on.
            Throwable cause = error instanceof CompletionException && error.getCause() != null
                    ? error.getCause()
                    : error;

            if (API_TIMEOUT.equals(cause.getMessage())) {
                Logger.error("Timed-out while waiting for {} with requestUuid {}", expectedTypeName, requestUuid);
            }

            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new CompletionException(cause);
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

