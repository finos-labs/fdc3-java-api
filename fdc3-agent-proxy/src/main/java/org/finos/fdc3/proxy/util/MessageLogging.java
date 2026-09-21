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

package org.finos.fdc3.proxy.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helpers for logging DACP messages without disclosing credentials or business data.
 * <p>
 * DACP messages carry both the WSCP {@code sharedSecret} and application context such as
 * contact and instrument data, so full message bodies are only ever written to the payload
 * trace logger (see {@link Logger#payload}). Routine logging uses {@link #summarise} instead.
 */
public final class MessageLogging {

    private static final String REDACTED = "<redacted>";

    /**
     * Keys whose values must never be logged. {@code sharedSecret} is the WSCP connection
     * credential, which the specification requires be kept out of logs.
     */
    private static final Set<String> SENSITIVE_KEYS = Set.of("sharedSecret");

    private MessageLogging() {
        // Utility class
    }

    /**
     * Describes a message by type and correlation id only, omitting its payload.
     *
     * @param message the DACP message, which may be null
     * @return a short description safe to log at DEBUG
     */
    @SuppressWarnings("unchecked")
    public static String summarise(Map<String, Object> message) {
        if (message == null) {
            return "<null message>";
        }

        Object type = message.get("type");
        StringBuilder description = new StringBuilder(type == null ? "<untyped>" : type.toString());

        Object metaValue = message.get("meta");
        if (metaValue instanceof Map) {
            Map<String, Object> meta = (Map<String, Object>) metaValue;
            appendIfPresent(description, "requestUuid", meta.get("requestUuid"));
            appendIfPresent(description, "responseUuid", meta.get("responseUuid"));
            appendIfPresent(description, "eventUuid", meta.get("eventUuid"));
            appendIfPresent(description, "connectionAttemptUuid", meta.get("connectionAttemptUuid"));
        }

        return description.toString();
    }

    private static void appendIfPresent(StringBuilder target, String label, Object value) {
        if (value != null) {
            target.append(' ').append(label).append('=').append(value);
        }
    }

    /**
     * Copies a message, replacing the value of every sensitive key with a placeholder.
     * <p>
     * The copy is structural only: values that are neither maps nor collections are shared
     * with the original, since they are not traversed further.
     *
     * @param message the DACP message, which may be null
     * @return a copy safe to serialise into the payload trace log
     */
    public static Object redact(Object message) {
        if (message instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> source = (Map<String, Object>) message;
            Map<String, Object> copy = new LinkedHashMap<>(source.size());
            for (Map.Entry<String, Object> entry : source.entrySet()) {
                if (SENSITIVE_KEYS.contains(entry.getKey())) {
                    copy.put(entry.getKey(), REDACTED);
                } else {
                    copy.put(entry.getKey(), redact(entry.getValue()));
                }
            }
            return copy;
        }

        if (message instanceof Collection) {
            Collection<?> source = (Collection<?>) message;
            List<Object> copy = new ArrayList<>(source.size());
            for (Object element : source) {
                copy.add(redact(element));
            }
            return copy;
        }

        return message;
    }
}
