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

import java.util.HashMap;
import java.util.Map;

import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.metadata.ContextMetadata;
import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.api.types.IntentHandler;
import org.finos.fdc3.proxy.Messaging;

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
}
