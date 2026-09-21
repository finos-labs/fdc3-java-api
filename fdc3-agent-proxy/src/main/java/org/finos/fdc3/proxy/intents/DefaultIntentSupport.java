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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.errors.ResolveError;
import org.finos.fdc3.api.errors.ResultError;
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
import org.finos.fdc3.proxy.util.ThrowIfUndefined;
import org.finos.fdc3.schema.*;

/**
 * Default implementation of IntentSupport.
 */
public class DefaultIntentSupport implements IntentSupport {

    private final Messaging messaging;
    private final IntentResolver intentResolver;
    private final long messageExchangeTimeout;
    private final long appLaunchTimeout;
    /**
     * Mutated on caller threads and iterated on the WebSocket receive thread, so it has to be
     * safe for concurrent traversal.
     */
    private final List<DefaultIntentListener> intentListeners = new CopyOnWriteArrayList<>();

    /** Guards the check-then-add in {@link #registerIntentListener}. */
    private final Object intentListenerLock = new Object();

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
    public CompletionStage<List<AppIntent>> findIntentsByContext(Context context, String resultType) {
        FindIntentsByContextRequest request = new FindIntentsByContextRequest();
        request.setType(FindIntentsByContextRequestType.FIND_INTENTS_BY_CONTEXT_REQUEST);
        request.setMeta(messaging.createMeta());

        FindIntentsByContextRequestPayload payload = new FindIntentsByContextRequestPayload();
        payload.setContext(context);
        payload.setResultType(resultType);
        request.setPayload(payload);

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        return messaging.<Map<String, Object>>exchange(requestMap, "findIntentsByContextResponse", messageExchangeTimeout)
                .thenApply(response -> {
                    FindIntentsByContextResponse typedResponse = messaging.getConverter()
                            .convertValue(response, FindIntentsByContextResponse.class);

                    if (typedResponse.getPayload() == null ||
                        typedResponse.getPayload().getAppIntents() == null ||
                        typedResponse.getPayload().getAppIntents().isEmpty()) {
                        throw new RuntimeException(ResolveError.NoAppsFound.toString());
                    }

                    return typedResponse.getPayload().getAppIntents();
                });
    }

    @Override
    public CompletionStage<IntentResolution> raiseIntent(
            String intent,
            Context context,
            AppIdentifier app,
            Boolean newInstance,
            AppProvidableContextMetadata metadata) {
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
            if (newInstance != null) {
                payloadMap.put("newInstance", newInstance);
            } else {
                payloadMap.remove("newInstance");
            }
            payloadMap.put("metadata", ContextMetadataMapper.toWireForIntentRequest(metadata, messaging::createUUID));
        }

        return messaging.<Map<String, Object>>exchange(requestMap, "raiseIntentResponse", appLaunchTimeout)
                .thenCompose(response -> {
                    RaiseIntentResponse typedResponse = messaging.getConverter()
                            .convertValue(response, RaiseIntentResponse.class);

                    AppIntent schemaAppIntent = typedResponse.getPayload() == null
                            ? null
                            : typedResponse.getPayload().getAppIntent();
                    org.finos.fdc3.schema.IntentResolution schemaIntentResolution =
                            typedResponse.getPayload() == null
                                    ? null
                                    : typedResponse.getPayload().getIntentResolution();

                    ThrowIfUndefined.throwIfUndefined(
                            schemaAppIntent != null ? schemaAppIntent : schemaIntentResolution,
                            "Invalid response from Desktop Agent to raiseIntent, either "
                                    + "payload.appIntent or payload.intentResolution must be set!",
                            response,
                            ResolveError.NoAppsFound.toString());

                    if (schemaAppIntent != null) {
                        return intentResolver.chooseIntent(List.of(schemaAppIntent), context)
                                .thenCompose(choice -> {
                                    if (choice == null) {
                                        throw new RuntimeException(ResolveError.UserCancelled.toString());
                                    }
                                    Boolean chosenNewInstance = choice.getAppId().getInstanceId() != null
                                            ? null
                                            : newInstance;
                                    return raiseIntent(
                                            intent, context, choice.getAppId(), chosenNewInstance, metadata);
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
    public CompletionStage<IntentResolution> raiseIntentForContext(
            Context context,
            AppIdentifier app,
            Boolean newInstance,
            AppProvidableContextMetadata metadata) {
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
            if (newInstance != null) {
                raiseForContextPayload.put("newInstance", newInstance);
            } else {
                raiseForContextPayload.remove("newInstance");
            }
            raiseForContextPayload.put("metadata", ContextMetadataMapper.toWireForIntentRequest(metadata, messaging::createUUID));
        }

        return messaging.<Map<String, Object>>exchange(requestMap, "raiseIntentForContextResponse", appLaunchTimeout)
                .thenCompose(response -> {
                    RaiseIntentForContextResponse typedResponse = messaging.getConverter()
                            .convertValue(response, RaiseIntentForContextResponse.class);

                    List<AppIntent> schemaAppIntents = typedResponse.getPayload() == null
                            ? null
                            : typedResponse.getPayload().getAppIntents();
                    org.finos.fdc3.schema.IntentResolution schemaIntentResolution =
                            typedResponse.getPayload() == null
                                    ? null
                                    : typedResponse.getPayload().getIntentResolution();

                    ThrowIfUndefined.throwIfUndefined(
                            schemaAppIntents != null && !schemaAppIntents.isEmpty()
                                    ? schemaAppIntents
                                    : schemaIntentResolution,
                            "Invalid response from Desktop Agent to raiseIntentForContext, either "
                                    + "payload.appIntents or payload.intentResolution must be set!",
                            response,
                            ResolveError.NoAppsFound.toString());

                    if (schemaAppIntents != null && !schemaAppIntents.isEmpty()) {
                        return intentResolver.chooseIntent(schemaAppIntents, context)
                                .thenCompose(choice -> {
                                    if (choice == null) {
                                        throw new RuntimeException(ResolveError.UserCancelled.toString());
                                    }
                                    Boolean chosenNewInstance = choice.getAppId().getInstanceId() != null
                                            ? null
                                            : newInstance;
                                    return raiseIntent(
                                            choice.getIntent(), context, choice.getAppId(), chosenNewInstance, metadata);
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
        return registerIntentListener(intent, null, handler);
    }

    @Override
    public CompletionStage<Listener> addIntentListenerWithContext(
            String intent, List<String> contextTypes, IntentHandler handler) {
        return registerIntentListener(intent, contextTypes, handler);
    }

    private CompletionStage<Listener> registerIntentListener(
            String intent, List<String> contextTypes, IntentHandler handler) {
        DefaultIntentListener[] holder = new DefaultIntentListener[1];

        // The conflict check and the insertion have to be one atomic step, otherwise two threads
        // registering the same intent can both see no conflict and both register.
        synchronized (intentListenerLock) {
            if (hasConflictingListener(intent, contextTypes)) {
                return CompletableFuture.failedFuture(
                        new RuntimeException(ResolveError.IntentListenerConflict.toString()));
            }

            holder[0] = new DefaultIntentListener(
                    messaging,
                    intent,
                    contextTypes,
                    handler,
                    messageExchangeTimeout,
                    () -> intentListeners.remove(holder[0]));
            intentListeners.add(holder[0]);
        }

        // If the Desktop Agent rejects the registration, undo the reservation made above.
        return holder[0].register()
                .handle((ignored, error) -> {
                    if (error != null) {
                        intentListeners.remove(holder[0]);
                        throw error instanceof RuntimeException
                                ? (RuntimeException) error
                                : new RuntimeException(error);
                    }
                    return (Listener) holder[0];
                });
    }

    private boolean hasConflictingListener(String intent, List<String> contextTypes) {
        return intentListeners.stream().anyMatch(existing -> {
            if (!existing.getIntent().equals(intent)) {
                return false;
            }
            List<String> existingTypes = existing.getContextTypes();
            if (existingTypes == null || contextTypes == null) {
                return true;
            }
            return existingTypes.stream().anyMatch(contextTypes::contains);
        });
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
                // Bounded deliberately. waitFor only schedules a timeout when this is positive,
                // so passing 0 left the listener registered forever whenever the resolving app
                // never sent a result.
                appLaunchTimeout,
                ResultError.ApiTimeout.toString()
        ).thenApply(response -> {
            metadataFuture.complete(extractResultMetadata(response, source));
            Map<String, Object> payload = (Map<String, Object>) response.get("payload");
            return convertIntentResult(payload.get("intentResult"));
        });

        // Without this the metadata stage stays pending forever when the result times out.
        result.whenComplete((ignored, error) -> {
            if (error != null) {
                metadataFuture.completeExceptionally(error);
            }
        });

        return new ResultPromises(result, metadataFuture);
    }

    @SuppressWarnings("unchecked")
    private static ContextMetadata extractResultMetadata(Map<String, Object> response, AppIdentifier source) {
        Map<String, Object> payload = (Map<String, Object>) response.get("payload");
        Map<String, Object> resultMetadata = payload != null
                ? (Map<String, Object>) payload.get("resultMetadata")
                : null;
        ContextMetadata metadata = ContextMetadataMapper.fromWire(
                resultMetadata, null, ContextMetadataMapper.MissingTraceId.EMPTY);
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
