/**
 * SPDX-License-Identifier: Apache-2.0
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

/**
 * Validates that a Desktop Agent's response carried the property the proxy needs.
 * <p>
 * The DACP schemas mark most response payload fields as optional, because a single message type
 * covers both success and failure. That means a response can be schema-valid yet still be missing
 * the field the caller depends on. Without a check, the proxy would go on to dereference it and
 * surface a {@code NullPointerException} rather than an FDC3 error, leaving an application unable
 * to tell a faulty Desktop Agent from a genuine failure.
 * <p>
 * This mirrors {@code throwIfUndefined} in the reference implementation: it exists to catch bugs
 * in Desktop Agent implementations, so the thrown message is always an FDC3 error name that the
 * calling application can match on, while the diagnostic detail goes to the log.
 */
public final class ThrowIfUndefined {

    private ThrowIfUndefined() {
        // Utility class
    }

    /**
     * Throws if {@code property} is absent.
     *
     * @param property      the value taken from the response payload
     * @param absentMessage what was expected, logged when the check fails
     * @param message       the message that was missing the property, logged for diagnosis
     * @param absentError   the FDC3 error name to report, which becomes the exception message
     * @throws RuntimeException carrying {@code absentError} if the property is absent
     */
    public static void throwIfUndefined(
            Object property, String absentMessage, Object message, String absentError) {
        if (property != null) {
            return;
        }

        Logger.error("{}. DACP message that resulted in the absent property: {}",
                absentMessage, MessageLogging.summarise(asMessageMap(message)));
        if (Logger.isPayloadEnabled()) {
            Logger.payload("Message with the absent property: {}", MessageLogging.redact(message));
        }

        throw new RuntimeException(absentError);
    }

    /**
     * Adapts a message to the map form {@link MessageLogging#summarise} expects, tolerating the
     * typed message objects that some call sites hold.
     */
    @SuppressWarnings("unchecked")
    private static java.util.Map<String, Object> asMessageMap(Object message) {
        if (message instanceof java.util.Map) {
            return (java.util.Map<String, Object>) message;
        }
        return null;
    }
}
