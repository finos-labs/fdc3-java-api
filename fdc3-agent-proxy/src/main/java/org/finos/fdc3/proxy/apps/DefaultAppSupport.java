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

package org.finos.fdc3.proxy.apps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.errors.OpenError;
import org.finos.fdc3.api.errors.ResolveError;
import org.finos.fdc3.api.metadata.AppMetadata;
import org.finos.fdc3.api.metadata.Icon;
import org.finos.fdc3.api.metadata.Image;
import org.finos.fdc3.api.metadata.ImplementationMetadata;
import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.proxy.util.Logger;

/**
 * Default implementation of AppSupport.
 */
public class DefaultAppSupport implements AppSupport {

    private final Messaging messaging;
    private final long messageExchangeTimeout;
    private final long appLaunchTimeout;

    public DefaultAppSupport(Messaging messaging, long messageExchangeTimeout, long appLaunchTimeout) {
        this.messaging = messaging;
        this.messageExchangeTimeout = messageExchangeTimeout;
        this.appLaunchTimeout = appLaunchTimeout;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<List<AppIdentifier>> findInstances(AppIdentifier app) {
        Map<String, Object> request = new HashMap<>();
        request.put("type", "findInstancesRequest");
        request.put("meta", messaging.createMeta());

        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> appMap = new HashMap<>();
        appMap.put("appId", app.getAppId());
        app.getInstanceId().ifPresent(id -> appMap.put("instanceId", id));
        payload.put("app", appMap);
        request.put("payload", payload);

        return messaging.<Map<String, Object>>exchange(request, "findInstancesResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    Map<String, Object> responsePayload = (Map<String, Object>) response.get("payload");
                    List<Map<String, Object>> identifiers = (List<Map<String, Object>>) responsePayload.get("appIdentifiers");

                    if (identifiers == null) {
                        return new ArrayList<>();
                    }

                    return identifiers.stream()
                            .map(id -> createAppIdentifier(id))
                            .collect(Collectors.toList());
                });
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<AppMetadata> getAppMetadata(AppIdentifier app) {
        Map<String, Object> request = new HashMap<>();
        request.put("type", "getAppMetadataRequest");
        request.put("meta", messaging.createMeta());

        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> appMap = new HashMap<>();
        appMap.put("appId", app.getAppId());
        app.getInstanceId().ifPresent(id -> appMap.put("instanceId", id));
        payload.put("app", appMap);
        request.put("payload", payload);

        return messaging.<Map<String, Object>>exchange(request, "getAppMetadataResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    Map<String, Object> responsePayload = (Map<String, Object>) response.get("payload");
                    Map<String, Object> appMetadataMap = (Map<String, Object>) responsePayload.get("appMetadata");

                    if (appMetadataMap == null) {
                        throw new RuntimeException(ResolveError.TargetAppUnavailable.toString());
                    }

                    return parseAppMetadata(appMetadataMap);
                });
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<AppIdentifier> open(AppIdentifier app, Context context) {
        Map<String, Object> request = new HashMap<>();
        request.put("type", "openRequest");
        request.put("meta", messaging.createMeta());

        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> appMap = new HashMap<>();
        appMap.put("appId", app.getAppId());
        app.getInstanceId().ifPresent(id -> appMap.put("instanceId", id));
        payload.put("app", appMap);
        if (context != null) {
            payload.put("context", context.toMap());
        }
        request.put("payload", payload);

        return messaging.<Map<String, Object>>exchange(request, "openResponse", appLaunchTimeout)
                .thenApply(response -> {
                    Map<String, Object> responsePayload = (Map<String, Object>) response.get("payload");
                    Map<String, Object> appIdentifierMap = (Map<String, Object>) responsePayload.get("appIdentifier");

                    if (appIdentifierMap == null) {
                        throw new RuntimeException(OpenError.AppNotFound.toString());
                    }

                    return createAppIdentifier(appIdentifierMap);
                });
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<ImplementationMetadata> getImplementationMetadata() {
        Map<String, Object> request = new HashMap<>();
        request.put("type", "getInfoRequest");
        request.put("meta", messaging.createMeta());
        request.put("payload", new HashMap<>());

        return messaging.<Map<String, Object>>exchange(request, "getInfoResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    Map<String, Object> responsePayload = (Map<String, Object>) response.get("payload");
                    Map<String, Object> implMetadata = (Map<String, Object>) responsePayload.get("implementationMetadata");

                    if (implMetadata != null) {
                        return parseImplementationMetadata(implMetadata);
                    } else {
                        Logger.error("Invalid response from Desktop Agent to getInfo!");
                        return createUnknownImplementationMetadata();
                    }
                });
    }

    private AppIdentifier createAppIdentifier(Map<String, Object> idMap) {
        String appId = (String) idMap.get("appId");
        String instanceId = (String) idMap.get("instanceId");
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

    @SuppressWarnings("unchecked")
    private ImplementationMetadata parseImplementationMetadata(Map<String, Object> implMap) {
        String fdc3Version = (String) implMap.get("fdc3Version");
        String provider = (String) implMap.get("provider");
        String providerVersion = (String) implMap.get("providerVersion");
        Map<String, Object> appMetadataMap = (Map<String, Object>) implMap.get("appMetadata");
        Map<String, Object> optionalFeaturesMap = (Map<String, Object>) implMap.get("optionalFeatures");

        AppMetadata appMetadata = appMetadataMap != null ? parseAppMetadata(appMetadataMap) : null;

        return new ImplementationMetadata() {
            @Override
            public String getFdc3Version() {
                return fdc3Version;
            }

            @Override
            public String getProvider() {
                return provider;
            }

            @Override
            public String getProviderVersion() {
                return providerVersion;
            }

            @Override
            public AppMetadata getAppMetadata() {
                return appMetadata;
            }

            @Override
            public OptionalFeatures getOptionalFeatures() {
                if (optionalFeaturesMap == null) {
                    return null;
                }
                return new OptionalFeatures() {
                    @Override
                    public boolean isOriginatingAppMetadata() {
                        Boolean val = (Boolean) optionalFeaturesMap.get("OriginatingAppMetadata");
                        return val != null && val;
                    }

                    @Override
                    public boolean isUserChannelMembershipAPIs() {
                        Boolean val = (Boolean) optionalFeaturesMap.get("UserChannelMembershipAPIs");
                        return val != null && val;
                    }

                    @Override
                    public boolean isDesktopAgentBridging() {
                        Boolean val = (Boolean) optionalFeaturesMap.get("DesktopAgentBridging");
                        return val != null && val;
                    }
                };
            }
        };
    }

    private ImplementationMetadata createUnknownImplementationMetadata() {
        return new ImplementationMetadata() {
            @Override
            public String getFdc3Version() {
                return "unknown";
            }

            @Override
            public String getProvider() {
                return "unknown";
            }

            @Override
            public String getProviderVersion() {
                return null;
            }

            @Override
            public AppMetadata getAppMetadata() {
                return new AppMetadata() {
                    @Override
                    public String getAppId() {
                        return "unknown";
                    }

                    @Override
                    public Optional<String> getInstanceId() {
                        return Optional.of("unknown");
                    }

                    @Override
                    public Optional<String> getName() {
                        return Optional.empty();
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
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String> getTooltip() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String> getDescription() {
                        return Optional.empty();
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

            @Override
            public OptionalFeatures getOptionalFeatures() {
                return new OptionalFeatures() {
                    @Override
                    public boolean isOriginatingAppMetadata() {
                        return false;
                    }

                    @Override
                    public boolean isUserChannelMembershipAPIs() {
                        return false;
                    }

                    @Override
                    public boolean isDesktopAgentBridging() {
                        return false;
                    }
                };
            }
        };
    }
}
