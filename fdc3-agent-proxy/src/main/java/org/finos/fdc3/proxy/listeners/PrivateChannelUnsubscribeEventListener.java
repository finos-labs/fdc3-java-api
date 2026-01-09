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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.finos.fdc3.api.types.EventHandler;
import org.finos.fdc3.api.types.FDC3Event;
import org.finos.fdc3.proxy.Messaging;
import org.finos.fdc3.proxy.util.Logger;

/**
 * Listener for private channel unsubscribe events.
 */
public class PrivateChannelUnsubscribeEventListener extends AbstractPrivateChannelEventListener {
    
    public PrivateChannelUnsubscribeEventListener(
            Messaging messaging,
            long messageExchangeTimeout,
            String channelId,
            EventHandler handler) {
        super(
            messaging,
            messageExchangeTimeout,
            channelId,
            Arrays.asList("privateChannelOnUnsubscribeEvent"),
            "unsubscribe",
            handler
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public void action(Map<String, Object> message) {
        String type = (String) message.get("type");
        
        if ("privateChannelOnUnsubscribeEvent".equals(type)) {
            Map<String, Object> payload = (Map<String, Object>) message.get("payload");
            Map<String, Object> details = new HashMap<>();
            details.put("contextType", payload.get("contextType"));
            
            FDC3Event event = new FDC3Event(FDC3Event.Type.ON_UNSUBSCRIBE, details);
            handler.handleEvent(event);
        } else {
            Logger.error("PrivateChannelUnsubscribeEventListener was called for a different message type: " + type);
        }
    }
}

