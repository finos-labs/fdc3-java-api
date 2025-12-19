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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.finos.fdc3.api.types.EventHandler;
import org.finos.fdc3.api.types.FDC3Event;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.proxy.util.Logger;

/**
 * Listener for all private channel events (null event type).
 */
public class PrivateChannelNullEventListener extends AbstractPrivateChannelEventListener {
    
    public PrivateChannelNullEventListener(
            Messaging messaging,
            long messageExchangeTimeout,
            String channelId,
            EventHandler handler) {
        super(
            messaging,
            messageExchangeTimeout,
            channelId,
            Arrays.asList(
                "privateChannelOnAddContextListenerEvent",
                "privateChannelOnUnsubscribeEvent",
                "privateChannelOnDisconnectEvent"
            ),
            null, // eventType is null for listening to all events
            handler
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public void action(Map<String, Object> message) {
        String type = (String) message.get("type");
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        
        FDC3Event.Type eventType;
        Map<String, Object> details;
        
        switch (type) {
            case "privateChannelOnAddContextListenerEvent":
                eventType = FDC3Event.Type.ADD_CONTEXT_LISTENER;
                details = new HashMap<>();
                details.put("contextType", payload.get("contextType"));
                break;
            case "privateChannelOnUnsubscribeEvent":
                eventType = FDC3Event.Type.ON_UNSUBSCRIBE;
                details = new HashMap<>();
                details.put("contextType", payload.get("contextType"));
                break;
            case "privateChannelOnDisconnectEvent":
                eventType = FDC3Event.Type.ON_DISCONNECT;
                details = null;
                break;
            default:
                Logger.error("PrivateChannelNullEventListener received unexpected message type: " + type);
                return;
        }
        
        FDC3Event event = new FDC3Event(eventType, details);
        handler.handleEvent(event);
    }
}

