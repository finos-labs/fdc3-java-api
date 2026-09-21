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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.finos.fdc3.api.errors.ChannelError;
import org.junit.jupiter.api.Test;

/**
 * Covers the guard every receive path uses when a Desktop Agent sends a response that is missing
 * a property the proxy needs.
 * <p>
 * Without this guard a malformed message produces a {@code NullPointerException} somewhere later,
 * which tells an application nothing. The contract is that the property's absence surfaces
 * immediately as the FDC3 error name the caller can match on.
 */
class ThrowIfUndefinedTest {

    private static Map<String, Object> message(Object payload) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "getOrCreateChannelResponse");
        message.put("payload", payload);
        return message;
    }

    @Test
    void absentPropertyThrowsTheFdc3ErrorName() {
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> ThrowIfUndefined.throwIfUndefined(
                        null,
                        "No channel returned",
                        message(new HashMap<>()),
                        ChannelError.NoChannelFound.toString()));

        // The message must be the bare error name: callers match on it, and anything else
        // (a sentence, a class name, a wrapped cause) breaks that.
        assertEquals("NoChannelFound", thrown.getMessage());
    }

    @Test
    void presentPropertyDoesNotThrow() {
        assertDoesNotThrow(() -> ThrowIfUndefined.throwIfUndefined(
                "channel-1",
                "No channel returned",
                message(Map.of("channel", "channel-1")),
                ChannelError.NoChannelFound.toString()));
    }

    @Test
    void falseAndEmptyAreStillPresentValues() {
        // Only absence is a failure. A legitimately false or empty value must pass, or the
        // guard would reject valid responses such as an empty channel-context list.
        assertDoesNotThrow(() -> ThrowIfUndefined.throwIfUndefined(
                Boolean.FALSE, "absent", message(new HashMap<>()), "ShouldNotThrow"));
        assertDoesNotThrow(() -> ThrowIfUndefined.throwIfUndefined(
                "", "absent", message(new HashMap<>()), "ShouldNotThrow"));
    }

    @Test
    void aMessageThatIsNotAMapStillReportsTheError() {
        // Some call sites hold a typed message object rather than a map. Diagnostics must
        // degrade rather than throw something unrelated from the logging path.
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> ThrowIfUndefined.throwIfUndefined(
                        null, "No listener UUID returned", new Object(), "MalformedMessage"));

        assertEquals("MalformedMessage", thrown.getMessage());
    }

    @Test
    void aNullMessageStillReportsTheError() {
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> ThrowIfUndefined.throwIfUndefined(
                        null, "Nothing to report against", null, "MalformedMessage"));

        assertEquals("MalformedMessage", thrown.getMessage());
    }
}
