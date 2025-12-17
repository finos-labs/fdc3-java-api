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

package org.finos.fdc3.proxy.intents;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.errors.ResolveError;
import org.finos.fdc3.api.metadata.AppIntent;
import org.finos.fdc3.api.metadata.AppMetadata;
import org.finos.fdc3.api.metadata.Icon;
import org.finos.fdc3.api.metadata.Image;
import org.finos.fdc3.api.metadata.IntentMetadata;
import org.finos.fdc3.api.metadata.IntentResolution;
import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.api.types.IntentHandler;
import org.finos.fdc3.api.types.Listener;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.proxy.listeners.DefaultIntentListener;
import org.finos.fdc3.schema.*;

/**
 * Default implementation of IntentSupport.
 */
public class DefaultIntentSupport implements IntentSupport {

    private final Messaging messaging;
    private final IntentResolver intentResolver;
    private final long messageExchangeTimeout;
    private final long appLaunchTimeout;

    public DefaultIntentSupport(
            Messaging messaging,
            IntentResolver intentResolver,
            long messageExchangeTimeout,
            long appLaunchTimeout) {
        this.messaging = messaging;
        this.intentResolver = intentResolver;
        this.messageExchangeTimeout = messageExchangeTimeout;
        this.appLaunchTimeout = appLaunchTimeout;
    }

    @Override
    public CompletionStage<AppIntent> findIntent(String intent, Context context, String resultType) {
        FindIntentRequest request = new FindIntentRequest();
        request.setType(FindIntentRequestType.FIND_INTENT_REQUEST);
        request.setMeta(messaging.createMeta());

        FindIntentRequestPayload payload = new FindIntentRequestPayload();
        payload.setIntent(intent);
        if (context != null) {
            payload.setContext(context);
        }
        if (resultType != null) {
            payload.setResultType(resultType);
        }
        request.setPayload(payload);

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        return messaging.<Map<String, Object>>exchange(requestMap, "findIntentResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    FindIntentResponse typedResponse = messaging.getConverter()
                            .convertValue(response, FindIntentResponse.class);

                    if (typedResponse.getPayload() == null ||
                        typedResponse.getPayload().getAppIntent() == null) {
                        throw new RuntimeException(ResolveError.NoAppsFound.toString());
                    }

                    AppIntent appIntent = toApiAppIntent(typedResponse.getPayload().getAppIntent());
                    if (appIntent.getApps().isEmpty()) {
                        throw new RuntimeException(ResolveError.NoAppsFound.toString());
                    }

                    return appIntent;
                });
    }

    @Override
    public CompletionStage<List<AppIntent>> findIntentsByContext(Context context) {
        FindIntentsByContextRequest request = new FindIntentsByContextRequest();
        request.setType(FindIntentsByContextRequestType.FIND_INTENTS_BY_CONTEXT_REQUEST);
        request.setMeta(messaging.createMeta());

        FindIntentsByContextRequestPayload payload = new FindIntentsByContextRequestPayload();
        payload.setContext(context);
        request.setPayload(payload);

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        return messaging.<Map<String, Object>>exchange(requestMap, "findIntentsByContextResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    FindIntentsByContextResponse typedResponse = messaging.getConverter()
                            .convertValue(response, FindIntentsByContextResponse.class);

                    if (typedResponse.getPayload() == null ||
                        typedResponse.getPayload().getAppIntents() == null ||
                        typedResponse.getPayload().getAppIntents().length == 0) {
                        throw new RuntimeException(ResolveError.NoAppsFound.toString());
                    }

                    return Arrays.stream(typedResponse.getPayload().getAppIntents())
                            .map(this::toApiAppIntent)
                            .collect(Collectors.toList());
                });
    }

    @Override
    public CompletionStage<IntentResolution> raiseIntent(String intent, Context context, AppIdentifier app) {
        AddContextListenerRequestMeta meta = messaging.createMeta();
        String requestUuid = meta.getRequestUUID();

        RaiseIntentRequest request = new RaiseIntentRequest();
        request.setType(RaiseIntentRequestType.RAISE_INTENT_REQUEST);
        request.setMeta(meta);

        RaiseIntentRequestPayload payload = new RaiseIntentRequestPayload();
        payload.setIntent(intent);
        payload.setContext(context);
        if (app != null) {
            payload.setApp(toSchemaAppIdentifier(app));
        }
        request.setPayload(payload);

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        CompletionStage<Object> resultPromise = createResultPromise(requestUuid);

        return messaging.<Map<String, Object>>exchange(requestMap, "raiseIntentResponse", appLaunchTimeout)
                .thenCompose(response -> {
                    RaiseIntentResponse typedResponse = messaging.getConverter()
                            .convertValue(response, RaiseIntentResponse.class);

                    if (typedResponse.getPayload() == null) {
                        throw new RuntimeException(ResolveError.NoAppsFound.toString());
                    }

                    org.finos.fdc3.schema.AppIntent schemaAppIntent = typedResponse.getPayload().getAppIntent();
                    org.finos.fdc3.schema.IntentResolution schemaIntentResolution = typedResponse.getPayload().getIntentResolution();

                    if (schemaAppIntent == null && schemaIntentResolution == null) {
                        throw new RuntimeException(ResolveError.NoAppsFound.toString());
                    }

                    if (schemaAppIntent != null) {
                        AppIntent appIntent = toApiAppIntent(schemaAppIntent);
                        return intentResolver.chooseIntent(List.of(appIntent), context)
                                .thenCompose(choice -> {
                                    if (choice == null) {
                                        throw new RuntimeException(ResolveError.UserCancelled.toString());
                                    }
                                    return raiseIntent(intent, context, choice.getAppId());
                                });
                    } else {
                        AppIdentifier source = toApiAppIdentifier(schemaIntentResolution.getSource());
                        String resolvedIntent = schemaIntentResolution.getIntent();

                        return java.util.concurrent.CompletableFuture.completedFuture(
                                new DefaultIntentResolution(messaging, messageExchangeTimeout, resultPromise, source, resolvedIntent));
                    }
                });
    }

    @Override
    public CompletionStage<IntentResolution> raiseIntentForContext(Context context, AppIdentifier app) {
        AddContextListenerRequestMeta meta = messaging.createMeta();
        String requestUuid = meta.getRequestUUID();

        RaiseIntentForContextRequest request = new RaiseIntentForContextRequest();
        request.setType(RaiseIntentForContextRequestType.RAISE_INTENT_FOR_CONTEXT_REQUEST);
        request.setMeta(meta);

        RaiseIntentForContextRequestPayload payload = new RaiseIntentForContextRequestPayload();
        payload.setContext(context);
        if (app != null) {
            payload.setApp(toSchemaAppIdentifier(app));
        }
        request.setPayload(payload);

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        CompletionStage<Object> resultPromise = createResultPromise(requestUuid);

        return messaging.<Map<String, Object>>exchange(requestMap, "raiseIntentForContextResponse", appLaunchTimeout)
                .thenCompose(response -> {
                    RaiseIntentForContextResponse typedResponse = messaging.getConverter()
                            .convertValue(response, RaiseIntentForContextResponse.class);

                    if (typedResponse.getPayload() == null) {
                        throw new RuntimeException(ResolveError.NoAppsFound.toString());
                    }

                    org.finos.fdc3.schema.AppIntent[] schemaAppIntents = typedResponse.getPayload().getAppIntents();
                    org.finos.fdc3.schema.IntentResolution schemaIntentResolution = typedResponse.getPayload().getIntentResolution();

                    if ((schemaAppIntents == null || schemaAppIntents.length == 0) && schemaIntentResolution == null) {
                        throw new RuntimeException(ResolveError.NoAppsFound.toString());
                    }

                    if (schemaAppIntents != null && schemaAppIntents.length > 0) {
                        List<AppIntent> appIntents = Arrays.stream(schemaAppIntents)
                                .map(this::toApiAppIntent)
                                .collect(Collectors.toList());

                        return intentResolver.chooseIntent(appIntents, context)
                                .thenCompose(choice -> {
                                    if (choice == null) {
                                        throw new RuntimeException(ResolveError.UserCancelled.toString());
                                    }
                                    return raiseIntent(choice.getIntent(), context, choice.getAppId());
                                });
                    } else {
                        AppIdentifier source = toApiAppIdentifier(schemaIntentResolution.getSource());
                        String resolvedIntent = schemaIntentResolution.getIntent();

                        return java.util.concurrent.CompletableFuture.completedFuture(
                                new DefaultIntentResolution(messaging, messageExchangeTimeout, resultPromise, source, resolvedIntent));
                    }
                });
    }

    @Override
    public CompletionStage<Listener> addIntentListener(String intent, IntentHandler handler) {
        DefaultIntentListener listener = new DefaultIntentListener(messaging, intent, handler, messageExchangeTimeout);
        return listener.register().thenApply(v -> listener);
    }

    // ============ Helper methods ============

    private org.finos.fdc3.schema.AppIdentifier toSchemaAppIdentifier(AppIdentifier app) {
        org.finos.fdc3.schema.AppIdentifier schemaApp = new org.finos.fdc3.schema.AppIdentifier();
        schemaApp.setAppID(app.getAppId());
        app.getInstanceId().ifPresent(schemaApp::setInstanceID);
        return schemaApp;
    }

    private AppIdentifier toApiAppIdentifier(org.finos.fdc3.schema.AppIdentifier schemaApp) {
        String appId = schemaApp.getAppID();
        String instanceId = schemaApp.getInstanceID();
        String desktopAgent = schemaApp.getDesktopAgent();
        return new AppIdentifier() {
            @Override
            public String getAppId() {
                return appId;
            }

            @Override
            public Optional<String> getInstanceId() {
                return Optional.ofNullable(instanceId);
            }

            @Override
            public Optional<String> getDesktopAgent() {
                return Optional.ofNullable(desktopAgent);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private CompletionStage<Object> createResultPromise(String requestUuid) {
        return messaging.<Map<String, Object>>waitFor(
                m -> {
                    String type = (String) m.get("type");
                    Map<String, Object> meta = (Map<String, Object>) m.get("meta");
                    String respRequestUuid = meta != null ? (String) meta.get("requestUuid") : null;
                    return "raiseIntentResultResponse".equals(type) && requestUuid.equals(respRequestUuid);
                },
                0,
                null
        ).thenApply(response -> {
            Map<String, Object> payload = (Map<String, Object>) response.get("payload");
            return payload.get("intentResult");
        });
    }

    private AppIntent toApiAppIntent(org.finos.fdc3.schema.AppIntent schemaAppIntent) {
        org.finos.fdc3.schema.IntentMetadata schemaIntent = schemaAppIntent.getIntent();
        org.finos.fdc3.schema.AppMetadata[] schemaApps = schemaAppIntent.getApps();

        String intentName = schemaIntent.getName();
        String displayName = schemaIntent.getDisplayName();

        IntentMetadata intent = new IntentMetadata() {
            @Override
            public String getName() {
                return intentName;
            }

            @Override
            public String getDisplayName() {
                return displayName != null ? displayName : intentName;
            }
        };

        List<AppMetadata> apps = Arrays.stream(schemaApps)
                .map(this::toApiAppMetadata)
                .collect(Collectors.toList());

        return new AppIntent() {
            @Override
            public IntentMetadata getIntent() {
                return intent;
            }

            @Override
            public List<AppMetadata> getApps() {
                return apps;
            }
        };
    }

    private AppMetadata toApiAppMetadata(org.finos.fdc3.schema.AppMetadata schemaApp) {
        String appId = schemaApp.getAppID();
        String instanceId = schemaApp.getInstanceID();
        String desktopAgent = schemaApp.getDesktopAgent();
        String name = schemaApp.getName();
        String title = schemaApp.getTitle();
        String description = schemaApp.getDescription();

        return new AppMetadata() {
            @Override
            public String getAppId() {
                return appId;
            }

            @Override
            public Optional<String> getInstanceId() {
                return Optional.ofNullable(instanceId);
            }

            @Override
            public Optional<String> getDesktopAgent() {
                return Optional.ofNullable(desktopAgent);
            }

            @Override
            public Optional<String> getName() {
                return Optional.ofNullable(name);
            }

            @Override
            public Optional<String> getVersion() {
                return Optional.empty();
            }

            @Override
            public Optional<Map<String, Object>> getInstanceMetadata() {
                return Optional.empty();
            }

            @Override
            public Optional<String> getTitle() {
                return Optional.ofNullable(title);
            }

            @Override
            public Optional<String> getTooltip() {
                return Optional.empty();
            }

            @Override
            public Optional<String> getDescription() {
                return Optional.ofNullable(description);
            }

            @Override
            public Optional<Collection<Icon>> getIcons() {
                return Optional.empty();
            }

            @Override
            public Optional<Collection<Image>> getScreenshots() {
                return Optional.empty();
            }

            @Override
            public Optional<String> getResultType() {
                return Optional.empty();
            }
        };
    }
}
