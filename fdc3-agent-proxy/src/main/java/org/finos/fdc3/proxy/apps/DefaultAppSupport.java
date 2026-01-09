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

package org.finos.fdc3.proxy.apps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.errors.OpenError;
import org.finos.fdc3.api.errors.ResolveError;
import org.finos.fdc3.api.metadata.AppMetadata;
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
        payload.setApp(app);
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

                    // AppMetadata extends AppIdentifier, so we can return directly
                    return Arrays.stream(typedResponse.getPayload().getAppIdentifiers())
                            .map(am -> (AppIdentifier) am)
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
        payload.setApp(app);
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

                    // Schema now uses fdc3-standard AppMetadata directly
                    return typedResponse.getPayload().getAppMetadata();
                });
    }

    @Override
    public CompletionStage<AppIdentifier> open(AppIdentifier app, Context context) {
        // Build typed request
        OpenRequest request = new OpenRequest();
        request.setType(OpenRequestType.OPEN_REQUEST);
        request.setMeta(messaging.createMeta());
        
        OpenRequestPayload payload = new OpenRequestPayload();
        payload.setApp(app);
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

                    return typedResponse.getPayload().getAppIdentifier();
                });
    }

    @Override
    @Deprecated
    public CompletionStage<AppIdentifier> open(String name, Context context) {
        // Create an AppIdentifier from the name string
        return open(new AppIdentifier(name), context);
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
                        // Schema now uses fdc3-standard ImplementationMetadata directly
                        return typedResponse.getPayload().getImplementationMetadata();
                    } else {
                        Logger.error("Invalid response from Desktop Agent to getInfo!");
                        return createUnknownImplementationMetadata();
                    }
                });
    }

    // ============ Conversion helpers ============
    // Schema now uses fdc3-standard AppIdentifier directly, no conversion needed

    private ImplementationMetadata createUnknownImplementationMetadata() {
        ImplementationMetadata result = new ImplementationMetadata();
        result.setFdc3Version("unknown");
        result.setProvider("unknown");

        AppMetadata appMetadata = new AppMetadata();
        appMetadata.setAppId("unknown");
        appMetadata.setInstanceId("unknown");
        result.setAppMetadata(appMetadata);

        ImplementationMetadata.OptionalFeatures optFeatures = new ImplementationMetadata.OptionalFeatures();
        result.setOptionalFeatures(optFeatures);

        return result;
    }
}
