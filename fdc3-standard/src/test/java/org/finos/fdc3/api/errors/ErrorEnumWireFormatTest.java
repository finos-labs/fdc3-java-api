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

package org.finos.fdc3.api.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Checks that every FDC3 error enum survives a trip through Jackson unchanged.
 * <p>
 * These enums cross the wire in DACP messages, so a constant whose serialized form does not match
 * the name the standard defines is not a cosmetic problem: an application matching on the error
 * would never see it. Because the whole point is to cover constants no other test exercises,
 * the cases are generated from {@code values()} rather than listed, so a constant added later is
 * covered without anyone remembering to add it here.
 */
class ErrorEnumWireFormatTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Every error enum in the API, with the reader needed to deserialize it. */
    private static final Class<?>[] ERROR_ENUMS = {
            AgentError.class,
            ChannelError.class,
            CloseError.class,
            OpenError.class,
            ResolveError.class,
            ResultError.class,
    };

    @TestFactory
    Stream<DynamicTest> everyConstantRoundTrips() {
        List<DynamicTest> tests = new ArrayList<>();
        for (Class<?> type : ERROR_ENUMS) {
            for (Object constant : type.getEnumConstants()) {
                tests.add(DynamicTest.dynamicTest(
                        type.getSimpleName() + "." + ((Enum<?>) constant).name(),
                        () -> assertRoundTrips(type, constant)));
            }
        }
        return tests.stream();
    }

    private static void assertRoundTrips(Class<?> type, Object constant) throws Exception {
        String json = MAPPER.writeValueAsString(constant);

        // Serializing must produce the wire value, not the Java constant name, and not an
        // object. Anything else means @JsonValue is missing or wrongly applied.
        assertEquals("\"" + constant + "\"", json,
                type.getSimpleName() + " must serialize to its wire value");

        assertSame(constant, MAPPER.readValue(json, type),
                type.getSimpleName() + " must deserialize back to the same constant");
    }

    @Test
    void unknownWireValuesAreRejected() {
        for (Class<?> type : ERROR_ENUMS) {
            // A Desktop Agent sending an error this version does not know about should surface
            // as a clear failure rather than a null that flows on to become something stranger.
            assertThrows(Exception.class,
                    () -> MAPPER.readValue("\"NoSuchErrorValue\"", type),
                    type.getSimpleName() + " must reject an unknown wire value");
        }
    }

    @Test
    void toStringIsTheWireValue() {
        // Error enums are routinely interpolated into exception messages and logs, so a
        // toString that differs from the wire value makes those unsearchable.
        assertEquals("AgentNotFound", AgentError.AgentNotFound.toString());
        assertEquals("NoAppsFound", ResolveError.NoAppsFound.toString());
        assertEquals("IntentHandlerRejected", ResultError.IntentHandlerRejected.toString());
    }
}
