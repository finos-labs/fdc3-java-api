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

package org.finos.fdc3.proxy.listeners;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.metadata.AppProvidableContextMetadata;
import org.finos.fdc3.api.metadata.ContextMetadata;
import org.finos.fdc3.api.types.ContextWithMetadata;
import org.finos.fdc3.api.types.IntentHandler;
import org.finos.fdc3.api.types.IntentResult;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.proxy.util.ContextMetadataMapper;
import org.finos.fdc3.schema.IntentEvent;
import org.finos.fdc3.schema.IntentResultRequest;
import org.finos.fdc3.schema.IntentResultRequestPayload;
import org.finos.fdc3.schema.IntentResultRequestType;

/**
 * Default implementation of an intent listener.
 * Extends AbstractListener to handle registration/unregistration.
 */
public class DefaultIntentListener extends AbstractListener<IntentHandler> {

    private final String intent;
    private final List<String> contextTypes;
    private final Runnable onUnsubscribe;

    public DefaultIntentListener(
            Messaging messaging,
            String intent,
            IntentHandler handler,
            long messageExchangeTimeout) {
        this(messaging, intent, null, handler, messageExchangeTimeout, null);
    }

    public DefaultIntentListener(
            Messaging messaging,
            String intent,
            List<String> contextTypes,
            IntentHandler handler,
            long messageExchangeTimeout,
            Runnable onUnsubscribe) {
        super(
            messaging,
            messageExchangeTimeout,
            handler,
            "addIntentListenerRequest",
            "addIntentListenerResponse",
            "intentListenerUnsubscribeRequest",
            "intentListenerUnsubscribeResponse"
        );
        this.intent = intent;
        this.contextTypes = contextTypes;
        this.onUnsubscribe = onUnsubscribe;
    }

    @Override
    protected Map<String, Object> buildSubscribeRequest() {
        Map<String, Object> request = new HashMap<>();
        Map<String, Object> payload = new HashMap<>();
        payload.put("intent", intent);
        if (contextTypes != null) {
            payload.put("contextTypes", contextTypes);
        }
        request.put("payload", payload);
        return request;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean filter(Map<String, Object> message) {
        String type = (String) message.get("type");
        if (!"intentEvent".equals(type)) {
            return false;
        }

        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        if (payload == null) {
            return false;
        }

        String msgIntent = (String) payload.get("intent");
        if (!intent.equals(msgIntent)) {
            return false;
        }

        if (contextTypes == null) {
            return true;
        }

        Object contextObj = payload.get("context");
        if (!(contextObj instanceof Map)) {
            return false;
        }
        String contextType = (String) ((Map<String, Object>) contextObj).get("type");
        return contextTypes.contains(contextType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void action(Map<String, Object> message) {
        IntentEvent intentEvent = messaging.getConverter().convertValue(message, IntentEvent.class);

        Context context = intentEvent.getPayload().getContext();
        Map<String, Object> messageMap = message;
        Map<String, Object> payloadMap = (Map<String, Object>) messageMap.get("payload");
        Map<String, Object> payloadMetadata = payloadMap != null
                ? (Map<String, Object>) payloadMap.get("metadata")
                : null;
        Object messageTimestamp = messageMap.get("meta") != null
                ? ((Map<String, Object>) messageMap.get("meta")).get("timestamp")
                : null;
        ContextMetadata contextMetadata = ContextMetadataMapper.fromWire(payloadMetadata, messageTimestamp);

        CompletionStage<Optional<Object>> resultFuture = handler.handleIntent(context, contextMetadata);
        handleIntentResult(resultFuture, intentEvent);
    }

    @Override
    public CompletionStage<Void> unsubscribe() {
        return super.unsubscribe().thenRun(() -> {
            if (onUnsubscribe != null) {
                onUnsubscribe.run();
            }
        });
    }

    public String getIntent() {
        return intent;
    }

    public List<String> getContextTypes() {
        return contextTypes;
    }

    private void handleIntentResult(
            CompletionStage<Optional<Object>> resultFuture,
            IntentEvent intentEvent) {

        resultFuture.thenAccept(optionalResult -> {
            UnwrappedIntentResult unwrapped = unwrapIntentResult(optionalResult.orElse(null));
            IntentResultRequest request = createIntentResultRequest(
                    unwrapped.result, unwrapped.appMetadata, intentEvent);

            Map<String, Object> requestMap = messaging.getConverter().toMap(request);
            if (unwrapped.appMetadata != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = (Map<String, Object>) requestMap.get("payload");
                if (payload != null) {
                    payload.put("metadata", ContextMetadataMapper.toWire(unwrapped.appMetadata));
                }
            }

            messaging.<Map<String, Object>>exchange(
                requestMap,
                "intentResultResponse",
                messageExchangeTimeout
            ).exceptionally(ex -> {
                System.err.println("Failed to send intent result: " + ex.getMessage());
                return null;
            });
        }).exceptionally(ex -> {
            IntentResultRequest request = createIntentResultRequest(null, null, intentEvent);
            Map<String, Object> requestMap = messaging.getConverter().toMap(request);

            messaging.<Map<String, Object>>exchange(
                requestMap,
                "intentResultResponse",
                messageExchangeTimeout
            ).exceptionally(ex2 -> {
                System.err.println("Failed to send intent result after error: " + ex2.getMessage());
                return null;
            });
            return null;
        });
    }

    private static final class UnwrappedIntentResult {
        final IntentResult result;
        final AppProvidableContextMetadata appMetadata;

        UnwrappedIntentResult(IntentResult result, AppProvidableContextMetadata appMetadata) {
            this.result = result;
            this.appMetadata = appMetadata;
        }
    }

    private static UnwrappedIntentResult unwrapIntentResult(Object raw) {
        if (raw instanceof ContextWithMetadata) {
            ContextWithMetadata cwm = (ContextWithMetadata) raw;
            return new UnwrappedIntentResult(cwm.getContext(), cwm.getMetadata());
        }
        if (raw instanceof IntentResult) {
            return new UnwrappedIntentResult((IntentResult) raw, null);
        }
        return new UnwrappedIntentResult(null, null);
    }

    private IntentResultRequest createIntentResultRequest(
            IntentResult apiResult,
            AppProvidableContextMetadata appMetadata,
            IntentEvent intentEvent) {

        IntentResultRequest request = new IntentResultRequest();
        request.setType(IntentResultRequestType.INTENT_RESULT_REQUEST);

        org.finos.fdc3.schema.AddContextListenerRequestMeta meta =
            new org.finos.fdc3.schema.AddContextListenerRequestMeta();
        meta.setRequestUUID(intentEvent.getMeta().getEventUUID());
        meta.setTimestamp(OffsetDateTime.now());
        request.setMeta(meta);

        IntentResultRequestPayload payload = new IntentResultRequestPayload();
        payload.setIntentEventUUID(intentEvent.getMeta().getEventUUID());
        payload.setRaiseIntentRequestUUID(intentEvent.getPayload().getRaiseIntentRequestUUID());
        payload.setIntentResult(convertIntentResult(apiResult));
        request.setPayload(payload);

        return request;
    }

    private org.finos.fdc3.schema.IntentResult convertIntentResult(IntentResult apiResult) {
        org.finos.fdc3.schema.IntentResult schemaResult = new org.finos.fdc3.schema.IntentResult();

        if (apiResult == null) {
            return schemaResult;
        }

        if (apiResult instanceof Context) {
            schemaResult.setContext((Context) apiResult);
        } else if (apiResult instanceof Channel) {
            Channel channel = (Channel) apiResult;
            org.finos.fdc3.schema.Channel schemaChannel = new org.finos.fdc3.schema.Channel();
            schemaChannel.setID(channel.getId());
            schemaChannel.setType(convertChannelType(channel.getType()));
            schemaChannel.setDisplayMetadata(null);
            schemaResult.setChannel(schemaChannel);
        }

        return schemaResult;
    }

    private org.finos.fdc3.schema.Type convertChannelType(Channel.Type type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case User:
                return org.finos.fdc3.schema.Type.USER;
            case App:
                return org.finos.fdc3.schema.Type.APP;
            case Private:
                return org.finos.fdc3.schema.Type.PRIVATE;
            default:
                return null;
        }
    }
}
