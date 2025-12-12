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
import java.util.Arrays;
import java.util.Collection;
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
import org.finos.fdc3.schema.*;

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
    public CompletionStage<List<AppIdentifier>> findInstances(AppIdentifier app) {
        // Build typed request
        FindInstancesRequest request = new FindInstancesRequest();
        request.setType(FindInstancesRequestType.FIND_INSTANCES_REQUEST);
        request.setMeta(messaging.createMeta());
        
        FindInstancesRequestPayload payload = new FindInstancesRequestPayload();
        payload.setApp(toSchemaAppIdentifier(app));
        request.setPayload(payload);

        // Convert to Map for messaging
        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        return messaging.<Map<String, Object>>exchange(requestMap, "findInstancesResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    FindInstancesResponse typedResponse = messaging.getConverter()
                            .convertValue(response, FindInstancesResponse.class);
                    
                    if (typedResponse.getPayload() == null || 
                        typedResponse.getPayload().getAppIdentifiers() == null) {
                        return new ArrayList<>();
                    }

                    return Arrays.stream(typedResponse.getPayload().getAppIdentifiers())
                            .map(this::toApiAppIdentifier)
                            .collect(Collectors.toList());
                });
    }

    @Override
    public CompletionStage<AppMetadata> getAppMetadata(AppIdentifier app) {
        // Build typed request
        GetAppMetadataRequest request = new GetAppMetadataRequest();
        request.setType(GetAppMetadataRequestType.GET_APP_METADATA_REQUEST);
        request.setMeta(messaging.createMeta());
        
        GetAppMetadataRequestPayload payload = new GetAppMetadataRequestPayload();
        payload.setApp(toSchemaAppIdentifier(app));
        request.setPayload(payload);

        // Convert to Map for messaging
        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        return messaging.<Map<String, Object>>exchange(requestMap, "getAppMetadataResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    GetAppMetadataResponse typedResponse = messaging.getConverter()
                            .convertValue(response, GetAppMetadataResponse.class);
                    
                    if (typedResponse.getPayload() == null || 
                        typedResponse.getPayload().getAppMetadata() == null) {
                        throw new RuntimeException(ResolveError.TargetAppUnavailable.toString());
                    }

                    return toApiAppMetadata(typedResponse.getPayload().getAppMetadata());
                });
    }

    @Override
    public CompletionStage<AppIdentifier> open(AppIdentifier app, Context context) {
        // Build typed request
        OpenRequest request = new OpenRequest();
        request.setType(OpenRequestType.OPEN_REQUEST);
        request.setMeta(messaging.createMeta());
        
        OpenRequestPayload payload = new OpenRequestPayload();
        payload.setApp(toSchemaAppIdentifier(app));
        if (context != null) {
            payload.setContext(context);
        }
        request.setPayload(payload);

        // Convert to Map for messaging
        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        return messaging.<Map<String, Object>>exchange(requestMap, "openResponse", appLaunchTimeout)
                .thenApply(response -> {
                    OpenResponse typedResponse = messaging.getConverter()
                            .convertValue(response, OpenResponse.class);
                    
                    if (typedResponse.getPayload() == null || 
                        typedResponse.getPayload().getAppIdentifier() == null) {
                        throw new RuntimeException(OpenError.AppNotFound.toString());
                    }

                    return toApiAppIdentifier(typedResponse.getPayload().getAppIdentifier());
                });
    }

    @Override
    public CompletionStage<ImplementationMetadata> getImplementationMetadata() {
        // Build typed request
        GetInfoRequest request = new GetInfoRequest();
        request.setType(GetInfoRequestType.GET_INFO_REQUEST);
        request.setMeta(messaging.createMeta());
        request.setPayload(new GetInfoRequestPayload());

        // Convert to Map for messaging
        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        return messaging.<Map<String, Object>>exchange(requestMap, "getInfoResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    GetInfoResponse typedResponse = messaging.getConverter()
                            .convertValue(response, GetInfoResponse.class);
                    
                    if (typedResponse.getPayload() != null && 
                        typedResponse.getPayload().getImplementationMetadata() != null) {
                        return toApiImplementationMetadata(typedResponse.getPayload().getImplementationMetadata());
                    } else {
                        Logger.error("Invalid response from Desktop Agent to getInfo!");
                        return createUnknownImplementationMetadata();
                    }
                });
    }

    // ============ Conversion helpers ============

    private org.finos.fdc3.schema.AppIdentifier toSchemaAppIdentifier(AppIdentifier app) {
        org.finos.fdc3.schema.AppIdentifier schemaApp = new org.finos.fdc3.schema.AppIdentifier();
        schemaApp.setAppID(app.getAppId());
        app.getInstanceId().ifPresent(schemaApp::setInstanceID);
        return schemaApp;
    }

    private AppIdentifier toApiAppIdentifier(org.finos.fdc3.schema.AppIdentifier schemaApp) {
        String appId = schemaApp.getAppID();
        String instanceId = schemaApp.getInstanceID();
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

    private AppIdentifier toApiAppIdentifier(org.finos.fdc3.schema.AppMetadata schemaApp) {
        String appId = schemaApp.getAppID();
        String instanceId = schemaApp.getInstanceID();
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

    private AppMetadata toApiAppMetadata(org.finos.fdc3.schema.AppMetadata schemaMetadata) {
        String appId = schemaMetadata.getAppID();
        String instanceId = schemaMetadata.getInstanceID();
        String name = schemaMetadata.getName();
        String title = schemaMetadata.getTitle();
        String description = schemaMetadata.getDescription();
        String version = schemaMetadata.getVersion();
        String tooltip = schemaMetadata.getTooltip();
        String resultType = schemaMetadata.getResultType();

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
                return Optional.ofNullable(version);
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
                return Optional.ofNullable(tooltip);
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
                return Optional.ofNullable(resultType);
            }
        };
    }

    private ImplementationMetadata toApiImplementationMetadata(org.finos.fdc3.schema.ImplementationMetadata schemaMetadata) {
        String fdc3Version = schemaMetadata.getFdc3Version();
        String provider = schemaMetadata.getProvider();
        String providerVersion = schemaMetadata.getProviderVersion();
        org.finos.fdc3.schema.AppMetadata schemaAppMetadata = schemaMetadata.getAppMetadata();
        org.finos.fdc3.schema.OptionalFeatures schemaOptionalFeatures = schemaMetadata.getOptionalFeatures();

        AppMetadata appMetadata = schemaAppMetadata != null ? toApiAppMetadata(schemaAppMetadata) : null;

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
                if (schemaOptionalFeatures == null) {
                    return null;
                }
                return new OptionalFeatures() {
                    @Override
                    public boolean isOriginatingAppMetadata() {
                        return schemaOptionalFeatures.getOriginatingAppMetadata();
                    }

                    @Override
                    public boolean isUserChannelMembershipAPIs() {
                        return schemaOptionalFeatures.getUserChannelMembershipAPIs();
                    }

                    @Override
                    public boolean isDesktopAgentBridging() {
                        return schemaOptionalFeatures.getDesktopAgentBridging();
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
