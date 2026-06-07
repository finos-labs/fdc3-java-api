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

package org.finos.fdc3.proxy.intents;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.errors.ResolveError;
import org.finos.fdc3.api.metadata.AppIntent;
import org.finos.fdc3.api.metadata.AppProvidableContextMetadata;
import org.finos.fdc3.api.metadata.ContextMetadata;
import org.finos.fdc3.api.metadata.IntentResolution;
import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.api.types.IntentHandler;
import org.finos.fdc3.api.types.IntentResult;
import org.finos.fdc3.api.types.Listener;
import org.finos.fdc3.api.ui.IntentResolver;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.proxy.channels.DefaultChannel;
import org.finos.fdc3.proxy.channels.DefaultPrivateChannel;
import org.finos.fdc3.proxy.listeners.DefaultIntentListener;
import org.finos.fdc3.proxy.util.ContextMetadataMapper;
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

                    // Schema types now use fdc3-standard types directly
                    AppIntent appIntent = typedResponse.getPayload().getAppIntent();
                    if (appIntent.getApps() == null || appIntent.getApps().length == 0) {
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

                    // Schema types now use fdc3-standard AppIntent directly
                    return Arrays.asList(typedResponse.getPayload().getAppIntents());
                });
    }

    @Override
    public CompletionStage<IntentResolution> raiseIntent(String intent, Context context, AppIdentifier app) {
        return raiseIntent(intent, context, app, null);
    }

    @Override
    public CompletionStage<IntentResolution> raiseIntent(
            String intent, Context context, AppIdentifier app, AppProvidableContextMetadata metadata) {
        AddContextListenerRequestMeta meta = messaging.createMeta();

        RaiseIntentRequest request = new RaiseIntentRequest();
        request.setType(RaiseIntentRequestType.RAISE_INTENT_REQUEST);
        request.setMeta(meta);

        RaiseIntentRequestPayload payload = new RaiseIntentRequestPayload();
        payload.setIntent(intent);
        payload.setContext(context);
        if (app != null) {
            payload.setApp(app);
        }
        request.setPayload(payload);

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);
        @SuppressWarnings("unchecked")
        Map<String, Object> payloadMap = (Map<String, Object>) requestMap.get("payload");
        if (payloadMap != null) {
            payloadMap.put("metadata", ContextMetadataMapper.toWireForIntentRequest(metadata, messaging::createUUID));
        }

        return messaging.<Map<String, Object>>exchange(requestMap, "raiseIntentResponse", appLaunchTimeout)
                .thenCompose(response -> {
                    RaiseIntentResponse typedResponse = messaging.getConverter()
                            .convertValue(response, RaiseIntentResponse.class);

                    if (typedResponse.getPayload() == null) {
                        throw new RuntimeException(ResolveError.NoAppsFound.toString());
                    }

                    AppIntent schemaAppIntent = typedResponse.getPayload().getAppIntent();
                    org.finos.fdc3.schema.IntentResolution schemaIntentResolution =
                            typedResponse.getPayload().getIntentResolution();

                    if (schemaAppIntent == null && schemaIntentResolution == null) {
                        throw new RuntimeException(ResolveError.NoAppsFound.toString());
                    }

                    if (schemaAppIntent != null) {
                        return intentResolver.chooseIntent(List.of(schemaAppIntent), context)
                                .thenCompose(choice -> {
                                    if (choice == null) {
                                        throw new RuntimeException(ResolveError.UserCancelled.toString());
                                    }
                                    return raiseIntent(intent, context, choice.getAppId(), metadata);
                                });
                    }

                    AppIdentifier source = schemaIntentResolution.getSource();
                    String resolvedIntent = schemaIntentResolution.getIntent();
                    ResultPromises promises = createResultPromises(meta.getRequestUUID(), source);
                    return CompletableFuture.completedFuture(new DefaultIntentResolution(
                            messaging,
                            messageExchangeTimeout,
                            promises.result,
                            promises.resultMetadata,
                            source,
                            resolvedIntent));
                });
    }

    @Override
    public CompletionStage<IntentResolution> raiseIntentForContext(Context context, AppIdentifier app) {
        return raiseIntentForContext(context, app, null);
    }

    @Override
    public CompletionStage<IntentResolution> raiseIntentForContext(
            Context context, AppIdentifier app, AppProvidableContextMetadata metadata) {
        AddContextListenerRequestMeta meta = messaging.createMeta();

        RaiseIntentForContextRequest request = new RaiseIntentForContextRequest();
        request.setType(RaiseIntentForContextRequestType.RAISE_INTENT_FOR_CONTEXT_REQUEST);
        request.setMeta(meta);

        RaiseIntentForContextRequestPayload payload = new RaiseIntentForContextRequestPayload();
        payload.setContext(context);
        if (app != null) {
            payload.setApp(app);
        }
        request.setPayload(payload);

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);
        @SuppressWarnings("unchecked")
        Map<String, Object> raiseForContextPayload = (Map<String, Object>) requestMap.get("payload");
        if (raiseForContextPayload != null) {
            raiseForContextPayload.put("metadata", ContextMetadataMapper.toWireForIntentRequest(metadata, messaging::createUUID));
        }

        return messaging.<Map<String, Object>>exchange(requestMap, "raiseIntentForContextResponse", appLaunchTimeout)
                .thenCompose(response -> {
                    RaiseIntentForContextResponse typedResponse = messaging.getConverter()
                            .convertValue(response, RaiseIntentForContextResponse.class);

                    if (typedResponse.getPayload() == null) {
                        throw new RuntimeException(ResolveError.NoAppsFound.toString());
                    }

                    AppIntent[] schemaAppIntents = typedResponse.getPayload().getAppIntents();
                    org.finos.fdc3.schema.IntentResolution schemaIntentResolution =
                            typedResponse.getPayload().getIntentResolution();

                    if ((schemaAppIntents == null || schemaAppIntents.length == 0) && schemaIntentResolution == null) {
                        throw new RuntimeException(ResolveError.NoAppsFound.toString());
                    }

                    if (schemaAppIntents != null && schemaAppIntents.length > 0) {
                        return intentResolver.chooseIntent(Arrays.asList(schemaAppIntents), context)
                                .thenCompose(choice -> {
                                    if (choice == null) {
                                        throw new RuntimeException(ResolveError.UserCancelled.toString());
                                    }
                                    return raiseIntent(choice.getIntent(), context, choice.getAppId(), metadata);
                                });
                    }

                    AppIdentifier source = schemaIntentResolution.getSource();
                    String resolvedIntent = schemaIntentResolution.getIntent();
                    ResultPromises promises = createResultPromises(meta.getRequestUUID(), source);
                    return CompletableFuture.completedFuture(new DefaultIntentResolution(
                            messaging,
                            messageExchangeTimeout,
                            promises.result,
                            promises.resultMetadata,
                            source,
                            resolvedIntent));
                });
    }

    @Override
    public CompletionStage<Listener> addIntentListener(String intent, IntentHandler handler) {
        DefaultIntentListener listener = new DefaultIntentListener(messaging, intent, handler, messageExchangeTimeout);
        return listener.register().thenApply(v -> listener);
    }

    private static final class ResultPromises {
        final CompletionStage<IntentResult> result;
        final CompletionStage<ContextMetadata> resultMetadata;

        ResultPromises(CompletionStage<IntentResult> result, CompletionStage<ContextMetadata> resultMetadata) {
            this.result = result;
            this.resultMetadata = resultMetadata;
        }
    }

    @SuppressWarnings("unchecked")
    private ResultPromises createResultPromises(String requestUuid, AppIdentifier source) {
        CompletableFuture<ContextMetadata> metadataFuture = new CompletableFuture<>();
        CompletionStage<IntentResult> result = messaging.<Map<String, Object>>waitFor(
                m -> {
                    String type = (String) m.get("type");
                    Map<String, Object> meta = (Map<String, Object>) m.get("meta");
                    String respRequestUuid = meta != null ? (String) meta.get("requestUuid") : null;
                    return "raiseIntentResultResponse".equals(type) && requestUuid.equals(respRequestUuid);
                },
                0,
                null
        ).thenApply(response -> {
            metadataFuture.complete(extractResultMetadata(response, source));
            Map<String, Object> payload = (Map<String, Object>) response.get("payload");
            return convertIntentResult(payload.get("intentResult"));
        });
        return new ResultPromises(result, metadataFuture);
    }

    @SuppressWarnings("unchecked")
    private static ContextMetadata extractResultMetadata(Map<String, Object> response, AppIdentifier source) {
        Map<String, Object> payload = (Map<String, Object>) response.get("payload");
        Map<String, Object> resultMetadata = payload != null
                ? (Map<String, Object>) payload.get("resultMetadata")
                : null;
        ContextMetadata metadata = ContextMetadataMapper.fromWire(resultMetadata, null);
        metadata.setSource(source);
        return metadata;
    }

    @SuppressWarnings("unchecked")
    private IntentResult convertIntentResult(Object intentResultObj) {
        if (intentResultObj == null) {
            return null;
        }
        if (!(intentResultObj instanceof Map)) {
            return null;
        }
        Map<String, Object> intentResult = (Map<String, Object>) intentResultObj;
        if (intentResult.isEmpty()) {
            return null;
        }
        Object contextObj = intentResult.get("context");
        if (contextObj != null) {
            if (contextObj instanceof Context) {
                return (Context) contextObj;
            }
            return Context.fromMap((Map<String, Object>) contextObj);
        }
        Object channelObj = intentResult.get("channel");
        if (channelObj instanceof Map) {
            return createChannel((Map<String, Object>) channelObj);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Channel createChannel(Map<String, Object> channelMap) {
        String id = (String) channelMap.get("id");
        Object typeObj = channelMap.get("type");
        Channel.Type type = Channel.Type.App;
        if (typeObj != null) {
            String typeStr = typeObj.toString();
            if ("user".equals(typeStr)) {
                type = Channel.Type.User;
            } else if ("private".equals(typeStr)) {
                type = Channel.Type.Private;
            }
        }
        Map<String, Object> displayMetadataMap = (Map<String, Object>) channelMap.get("displayMetadata");
        org.finos.fdc3.api.metadata.DisplayMetadata displayMetadata = null;
        if (displayMetadataMap != null) {
            displayMetadata = org.finos.fdc3.api.metadata.DisplayMetadata.fromMap(displayMetadataMap);
        }
        if (type == Channel.Type.Private) {
            return new DefaultPrivateChannel(messaging, messageExchangeTimeout, id);
        }
        return new DefaultChannel(messaging, messageExchangeTimeout, id, type, displayMetadata);
    }
}
