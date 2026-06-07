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

package org.finos.fdc3.api.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Type representing the event object passed to event handlers subscribed to
 * FDC3 events via the {@code addEventListener} method.
 * 
 * Events will always include both {@code type} and {@code details} properties,
 * which describe the type of event and any additional details, respectively.
 */
public class FDC3Event {

    /**
     * Enumeration of FDC3 event types.
     */
    public enum Type {
        ADD_CONTEXT_LISTENER("addContextListener"),
        ON_UNSUBSCRIBE("onUnsubscribe"),
        ON_DISCONNECT("onDisconnect"),
        USER_CHANNEL_CHANGED("userChannelChanged"),
        CONTEXT_CLEARED("contextCleared");


        private final String value;

        Type(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @JsonCreator
        public static Type fromValue(String value) {
            for (Type type : Type.values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown FDC3 event type: " + value);
        }

        /**
         * Maps a DACP agent event message type to the corresponding API event type,
         * or {@code null} if the message is not a recognised FDC3 event.
         */
        public static Type fromMessageType(String messageType) {
            if (messageType == null) {
                return null;
            }
            switch (messageType) {
                case "channelChangedEvent":
                    return USER_CHANNEL_CHANGED;
                case "contextClearedEvent":
                    return CONTEXT_CLEARED;
                case "privateChannelOnAddContextListenerEvent":
                    return ADD_CONTEXT_LISTENER;
                case "privateChannelOnUnsubscribeEvent":
                    return ON_UNSUBSCRIBE;
                case "privateChannelOnDisconnectEvent":
                    return ON_DISCONNECT;
                default:
                    return null;
            }
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private final Type type;
    private final Object details;

    @JsonCreator
    public FDC3Event(@JsonProperty("type") Type type, @JsonProperty("details") Object details) {
        this.type = type;
        this.details = details;
    }

    /**
     * Returns the typed enum constant for this event, or {@code null} for
     * wildcard listeners receiving unmapped agent events.
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the details object containing additional information about the event.
     */
    public Object getDetails() {
        return details;
    }
}
