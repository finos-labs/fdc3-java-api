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
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.metadata.ContextMetadata;
import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.api.types.IntentHandler;
import org.finos.fdc3.api.types.Listener;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.schema.*;

/**
 * Default implementation of an intent listener.
 */
public class DefaultIntentListener implements RegisterableListener, Listener {

    private final Messaging messaging;
    private final String intent;
    private final IntentHandler handler;
    private final long messageExchangeTimeout;
    private final String id;

    public DefaultIntentListener(
            Messaging messaging,
            String intent,
            IntentHandler handler,
            long messageExchangeTimeout) {
        this.messaging = messaging;
        this.intent = intent;
        this.handler = handler;
        this.messageExchangeTimeout = messageExchangeTimeout;
        this.id = messaging.createUUID();
    }

    @Override
    public String getId() {
        return id;
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
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        Map<String, Object> contextMap = (Map<String, Object>) payload.get("context");
        Context context = Context.fromMap(contextMap);

        // Create context metadata from the message
        Map<String, Object> meta = (Map<String, Object>) message.get("meta");
        Map<String, Object> sourceMap = (Map<String, Object>) meta.get("source");

        ContextMetadata contextMetadata = null;
        if (sourceMap != null) {
            String sourceAppId = (String) sourceMap.get("appId");
            String sourceInstanceId = (String) sourceMap.get("instanceId");
            contextMetadata = new ContextMetadata() {
                @Override
                public AppIdentifier getSource() {
                    return new AppIdentifier() {
                        @Override
                        public String getAppId() {
                            return sourceAppId;
                        }

                        @Override
                        public java.util.Optional<String> getInstanceId() {
                            return java.util.Optional.ofNullable(sourceInstanceId);
                        }
                    };
                }
            };
        }

        handler.handleIntent(context, contextMetadata);
    }

    @Override
    public CompletionStage<Void> register() {
        AddIntentListenerRequest request = new AddIntentListenerRequest();
        request.setType(AddIntentListenerRequestType.ADD_INTENT_LISTENER_REQUEST);
        request.setMeta(createMeta());

        AddIntentListenerRequestPayload payload = new AddIntentListenerRequestPayload();
        payload.setIntent(intent);
        request.setPayload(payload);

        Map<String, Object> requestMap = messaging.getConverter().toMap(request);

        messaging.register(this);

        return messaging.<Map<String, Object>>exchange(requestMap, "addIntentListenerResponse", messageExchangeTimeout)
                .thenApply(response -> null);
    }

    @Override
    public void unsubscribe() {
        messaging.unregister(id);
    }

    private AddContextListenerRequestMeta createMeta() {
        AddContextListenerRequestMeta meta = new AddContextListenerRequestMeta();
        meta.setRequestUUID(messaging.createUUID());
        meta.setTimestamp(OffsetDateTime.now());

        AppIdentifier appId = messaging.getAppIdentifier();
        if (appId != null) {
            org.finos.fdc3.schema.AppIdentifier source = new org.finos.fdc3.schema.AppIdentifier();
            source.setAppID(appId.getAppId());
            appId.getInstanceId().ifPresent(source::setInstanceID);
            meta.setSource(source);
        }
        return meta;
    }
}
