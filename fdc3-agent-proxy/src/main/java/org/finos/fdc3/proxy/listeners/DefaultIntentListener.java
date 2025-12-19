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

package org.finos.fdc3.proxy.listeners;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.metadata.ContextMetadata;
import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.api.types.IntentHandler;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.schema.IntentEvent;
import org.finos.fdc3.schema.IntentResult;
import org.finos.fdc3.schema.IntentResultRequest;
import org.finos.fdc3.schema.IntentResultRequestPayload;
import org.finos.fdc3.schema.IntentResultRequestType;
import org.finos.fdc3.schema.IntentResultResponse;

/**
 * Default implementation of an intent listener.
 * Extends AbstractListener to handle registration/unregistration.
 */
public class DefaultIntentListener extends AbstractListener<IntentHandler> {

    private final String intent;

    public DefaultIntentListener(
            Messaging messaging,
            String intent,
            IntentHandler handler,
            long messageExchangeTimeout) {
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
    }

    @Override
    protected Map<String, Object> buildSubscribeRequest() {
        Map<String, Object> request = new HashMap<>();
        Map<String, Object> payload = new HashMap<>();
        payload.put("intent", intent);
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
        return intent.equals(msgIntent);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void action(Map<String, Object> message) {
        // Convert the message to typed IntentEvent
        IntentEvent intentEvent = messaging.getConverter().convertValue(message, IntentEvent.class);
        
        Context context = intentEvent.getPayload().getContext();
        AppIdentifier originatingApp = intentEvent.getPayload().getOriginatingApp();

        ContextMetadata contextMetadata = new ContextMetadata();
        contextMetadata.setSource(originatingApp);

        // Call the handler and get the result
        CompletionStage<Optional<org.finos.fdc3.api.types.IntentResult>> resultFuture = 
            handler.handleIntent(context, contextMetadata);

        // Handle the intent result
        handleIntentResult(resultFuture, intentEvent);
    }

    private void handleIntentResult(
            CompletionStage<Optional<org.finos.fdc3.api.types.IntentResult>> resultFuture, 
            IntentEvent intentEvent) {
        
        resultFuture.thenAccept(optionalResult -> {
            IntentResultRequest request = createIntentResultRequest(optionalResult.orElse(null), intentEvent);
            
            // Convert to Map and send
            Map<String, Object> requestMap = messaging.getConverter().toMap(request);
            
            messaging.<Map<String, Object>>exchange(
                requestMap, 
                "intentResultResponse", 
                messageExchangeTimeout
            ).exceptionally(ex -> {
                // Log error but don't fail
                System.err.println("Failed to send intent result: " + ex.getMessage());
                return null;
            });
        }).exceptionally(ex -> {
            // Handler threw an exception, send empty result
            IntentResultRequest request = createIntentResultRequest(null, intentEvent);
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

    private IntentResultRequest createIntentResultRequest(
            org.finos.fdc3.api.types.IntentResult apiResult, 
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

    private IntentResult convertIntentResult(org.finos.fdc3.api.types.IntentResult apiResult) {
        IntentResult schemaResult = new IntentResult();
        
        if (apiResult == null) {
            // Void result - return empty IntentResult
            return schemaResult;
        }
        
        if (apiResult instanceof Context) {
            schemaResult.setContext((Context) apiResult);
        } else if (apiResult instanceof Channel) {
            Channel channel = (Channel) apiResult;
            org.finos.fdc3.schema.Channel schemaChannel = new org.finos.fdc3.schema.Channel();
            schemaChannel.setID(channel.getId());
            schemaChannel.setType(convertChannelType(channel.getType()));
            // DisplayMetadata is not part of the Channel API interface,
            // only available on UserChannel/PrivateChannel implementations
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
