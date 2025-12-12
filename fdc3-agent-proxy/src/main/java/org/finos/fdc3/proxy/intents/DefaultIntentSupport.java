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

import java.util.Collection;
import java.util.HashMap;
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
    @SuppressWarnings("unchecked")
    public CompletionStage<AppIntent> findIntent(String intent, Context context, String resultType) {
        Map<String, Object> request = new HashMap<>();
        request.put("type", "findIntentRequest");
        request.put("meta", messaging.createMeta());

        Map<String, Object> payload = new HashMap<>();
        payload.put("intent", intent);
        if (context != null) {
            payload.put("context", context.toMap());
        }
        if (resultType != null) {
            payload.put("resultType", resultType);
        }
        request.put("payload", payload);

        return messaging.<Map<String, Object>>exchange(request, "findIntentResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    Map<String, Object> responsePayload = (Map<String, Object>) response.get("payload");
                    Map<String, Object> appIntentMap = (Map<String, Object>) responsePayload.get("appIntent");

                    if (appIntentMap == null) {
                        throw new RuntimeException(ResolveError.NoAppsFound.toString());
                    }

                    AppIntent appIntent = parseAppIntent(appIntentMap);
                    if (appIntent.getApps().isEmpty()) {
                        throw new RuntimeException(ResolveError.NoAppsFound.toString());
                    }

                    return appIntent;
                });
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<List<AppIntent>> findIntentsByContext(Context context) {
        Map<String, Object> request = new HashMap<>();
        request.put("type", "findIntentsByContextRequest");
        request.put("meta", messaging.createMeta());

        Map<String, Object> payload = new HashMap<>();
        payload.put("context", context.toMap());
        request.put("payload", payload);

        return messaging.<Map<String, Object>>exchange(request, "findIntentsByContextResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    Map<String, Object> responsePayload = (Map<String, Object>) response.get("payload");
                    List<Map<String, Object>> appIntentsList = (List<Map<String, Object>>) responsePayload.get("appIntents");

                    if (appIntentsList == null || appIntentsList.isEmpty()) {
                        throw new RuntimeException(ResolveError.NoAppsFound.toString());
                    }

                    return appIntentsList.stream()
                            .map(this::parseAppIntent)
                            .collect(Collectors.toList());
                });
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<IntentResolution> raiseIntent(String intent, Context context, AppIdentifier app) {
        Map<String, Object> meta = messaging.createMeta();
        String requestUuid = (String) meta.get("requestUuid");

        Map<String, Object> request = new HashMap<>();
        request.put("type", "raiseIntentRequest");
        request.put("meta", meta);

        Map<String, Object> payload = new HashMap<>();
        payload.put("intent", intent);
        payload.put("context", context.toMap());
        if (app != null) {
            Map<String, Object> appMap = new HashMap<>();
            appMap.put("appId", app.getAppId());
            app.getInstanceId().ifPresent(id -> appMap.put("instanceId", id));
            payload.put("app", appMap);
        }
        request.put("payload", payload);

        CompletionStage<Object> resultPromise = createResultPromise(requestUuid);

        return messaging.<Map<String, Object>>exchange(request, "raiseIntentResponse", appLaunchTimeout)
                .thenCompose(response -> {
                    Map<String, Object> responsePayload = (Map<String, Object>) response.get("payload");
                    Map<String, Object> appIntentMap = (Map<String, Object>) responsePayload.get("appIntent");
                    Map<String, Object> intentResolutionMap = (Map<String, Object>) responsePayload.get("intentResolution");

                    if (appIntentMap == null && intentResolutionMap == null) {
                        throw new RuntimeException(ResolveError.NoAppsFound.toString());
                    }

                    if (appIntentMap != null) {
                        AppIntent appIntent = parseAppIntent(appIntentMap);
                        return intentResolver.chooseIntent(List.of(appIntent), context)
                                .thenCompose(choice -> {
                                    if (choice == null) {
                                        throw new RuntimeException(ResolveError.UserCancelled.toString());
                                    }
                                    return raiseIntent(intent, context, choice.getAppId());
                                });
                    } else {
                        Map<String, Object> sourceMap = (Map<String, Object>) intentResolutionMap.get("source");
                        String resolvedIntent = (String) intentResolutionMap.get("intent");
                        AppIdentifier source = createAppIdentifier(sourceMap);

                        return java.util.concurrent.CompletableFuture.completedFuture(
                                new DefaultIntentResolution(messaging, resultPromise, source, resolvedIntent));
                    }
                });
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<IntentResolution> raiseIntentForContext(Context context, AppIdentifier app) {
        Map<String, Object> meta = messaging.createMeta();
        String requestUuid = (String) meta.get("requestUuid");

        Map<String, Object> request = new HashMap<>();
        request.put("type", "raiseIntentForContextRequest");
        request.put("meta", meta);

        Map<String, Object> payload = new HashMap<>();
        payload.put("context", context.toMap());
        if (app != null) {
            Map<String, Object> appMap = new HashMap<>();
            appMap.put("appId", app.getAppId());
            app.getInstanceId().ifPresent(id -> appMap.put("instanceId", id));
            payload.put("app", appMap);
        }
        request.put("payload", payload);

        CompletionStage<Object> resultPromise = createResultPromise(requestUuid);

        return messaging.<Map<String, Object>>exchange(request, "raiseIntentForContextResponse", appLaunchTimeout)
                .thenCompose(response -> {
                    Map<String, Object> responsePayload = (Map<String, Object>) response.get("payload");
                    List<Map<String, Object>> appIntentsList = (List<Map<String, Object>>) responsePayload.get("appIntents");
                    Map<String, Object> intentResolutionMap = (Map<String, Object>) responsePayload.get("intentResolution");

                    if ((appIntentsList == null || appIntentsList.isEmpty()) && intentResolutionMap == null) {
                        throw new RuntimeException(ResolveError.NoAppsFound.toString());
                    }

                    if (appIntentsList != null && !appIntentsList.isEmpty()) {
                        List<AppIntent> appIntents = appIntentsList.stream()
                                .map(this::parseAppIntent)
                                .collect(Collectors.toList());

                        return intentResolver.chooseIntent(appIntents, context)
                                .thenCompose(choice -> {
                                    if (choice == null) {
                                        throw new RuntimeException(ResolveError.UserCancelled.toString());
                                    }
                                    return raiseIntent(choice.getIntent(), context, choice.getAppId());
                                });
                    } else {
                        Map<String, Object> sourceMap = (Map<String, Object>) intentResolutionMap.get("source");
                        String resolvedIntent = (String) intentResolutionMap.get("intent");
                        AppIdentifier source = createAppIdentifier(sourceMap);

                        return java.util.concurrent.CompletableFuture.completedFuture(
                                new DefaultIntentResolution(messaging, resultPromise, source, resolvedIntent));
                    }
                });
    }

    @Override
    public CompletionStage<Listener> addIntentListener(String intent, IntentHandler handler) {
        DefaultIntentListener listener = new DefaultIntentListener(messaging, intent, handler, messageExchangeTimeout);
        return listener.register().thenApply(v -> listener);
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

    private AppIdentifier createAppIdentifier(Map<String, Object> sourceMap) {
        String appId = (String) sourceMap.get("appId");
        String instanceId = (String) sourceMap.get("instanceId");
        return new AppIdentifier() {
            @Override
            public String getAppId() {
                return appId;
            }

            @Override
            public Optional<String> getInstanceId() {
                return Optional.ofNullable(instanceId);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private AppIntent parseAppIntent(Map<String, Object> appIntentMap) {
        Map<String, Object> intentMap = (Map<String, Object>) appIntentMap.get("intent");
        List<Map<String, Object>> appsList = (List<Map<String, Object>>) appIntentMap.get("apps");

        String intentName = (String) intentMap.get("name");
        String displayName = (String) intentMap.get("displayName");

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

        List<AppMetadata> apps = appsList.stream()
                .map(this::parseAppMetadata)
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

    private AppMetadata parseAppMetadata(Map<String, Object> appMap) {
        String appId = (String) appMap.get("appId");
        String instanceId = (String) appMap.get("instanceId");
        String name = (String) appMap.get("name");
        String title = (String) appMap.get("title");
        String description = (String) appMap.get("description");

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
