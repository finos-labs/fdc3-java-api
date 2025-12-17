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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.metadata.DisplayMetadata;
import org.finos.fdc3.api.metadata.IntentResolution;
import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.api.types.IntentResult;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.proxy.channels.DefaultChannel;
import org.finos.fdc3.proxy.channels.DefaultPrivateChannel;

/**
 * Default implementation of IntentResolution.
 */
public class DefaultIntentResolution implements IntentResolution {

    private final Messaging messaging;
    private final long messageExchangeTimeout;
    private final CompletionStage<Object> resultPromise;
    private final AppIdentifier source;
    private final String intent;

    public DefaultIntentResolution(
            Messaging messaging,
            long messageExchangeTimeout,
            CompletionStage<Object> resultPromise,
            AppIdentifier source,
            String intent) {
        this.messaging = messaging;
        this.messageExchangeTimeout = messageExchangeTimeout;
        this.resultPromise = resultPromise;
        this.source = source;
        this.intent = intent;
    }

    @Override
    public AppIdentifier getSource() {
        return source;
    }

    @Override
    public String getIntent() {
        return intent;
    }

    @Override
    public Optional<String> getVersion() {
        return Optional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<IntentResult> getResult() {
        return resultPromise.thenApply(result -> {
            // Return null when there's no result (void)
            if (result == null) {
                return (IntentResult) null;
            }
            
            // The result from the Desktop Agent is a map with either 'context' or 'channel' key
            if (result instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) result;
                
                // Empty map means void result
                if (resultMap.isEmpty()) {
                    return (IntentResult) null;
                }
                
                // Check for context result
                Object contextObj = resultMap.get("context");
                if (contextObj != null) {
                    if (contextObj instanceof Context) {
                        return (IntentResult) contextObj;
                    } else if (contextObj instanceof Map) {
                        return (IntentResult) Context.fromMap((Map<String, Object>) contextObj);
                    }
                }
                
                // Check for channel result
                Object channelObj = resultMap.get("channel");
                if (channelObj != null && channelObj instanceof Map) {
                    Map<String, Object> channelMap = (Map<String, Object>) channelObj;
                    return (IntentResult) createChannel(channelMap);
                }
            }
            
            // If result is already a Context or Channel, return it directly
            if (result instanceof IntentResult) {
                return (IntentResult) result;
            }
            
            return (IntentResult) null;
        });
    }
    
    @SuppressWarnings("unchecked")
    private Channel createChannel(Map<String, Object> channelMap) {
        String id = (String) channelMap.get("id");
        Object typeObj = channelMap.get("type");
        
        Channel.Type type;
        if (typeObj instanceof Channel.Type) {
            type = (Channel.Type) typeObj;
        } else {
            String typeStr = typeObj != null ? typeObj.toString() : null;
            if ("user".equals(typeStr)) {
                type = Channel.Type.User;
            } else if ("app".equals(typeStr)) {
                type = Channel.Type.App;
            } else if ("private".equals(typeStr)) {
                type = Channel.Type.Private;
            } else {
                type = Channel.Type.App; // default
            }
        }
        
        Map<String, Object> displayMetadataMap = (Map<String, Object>) channelMap.get("displayMetadata");
        DisplayMetadata displayMetadata = null;
        if (displayMetadataMap != null) {
            displayMetadata = new DisplayMetadata(
                    (String) displayMetadataMap.get("name"),
                    (String) displayMetadataMap.get("color"),
                    (String) displayMetadataMap.get("glyph")
            );
        }
        
        // Use existing channel implementations
        if (type == Channel.Type.Private) {
            return new DefaultPrivateChannel(messaging, messageExchangeTimeout, id);
        } else {
            return new DefaultChannel(messaging, messageExchangeTimeout, id, type, displayMetadata);
        }
    }
}
