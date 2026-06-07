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

        @Override
        public String toString() {
            return value;
        }
    }

    private final Type type;
    private final String wireType;
    private final Object details;

    public FDC3Event(Type type, Object details) {
        this.type = type;
        this.wireType = null;
        this.details = details;
    }

    private FDC3Event(String wireType, Object details) {
        this.type = null;
        this.wireType = wireType;
        this.details = details;
    }

    /**
     * Creates an event forwarded from a DACP wire message (for wildcard listeners).
     */
    public static FDC3Event fromWire(String wireMessageType, Object details) {
        return new FDC3Event(wireMessageType, details);
    }

    /**
     * Returns the typed enum constant for this event, or {@code null} for wire-forwarded events.
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the event type as a string (API or wire message type).
     */
    public String getTypeString() {
        return type != null ? type.getValue() : wireType;
    }

    /**
     * Returns the details object containing additional information about the event.
     */
    public Object getDetails() {
        return details;
    }
}
