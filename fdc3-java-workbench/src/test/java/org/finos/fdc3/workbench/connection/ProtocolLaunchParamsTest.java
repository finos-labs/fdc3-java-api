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

package org.finos.fdc3.workbench.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class ProtocolLaunchParamsTest {

    @Test
    void parsesLaunchUri() {
        Optional<ProtocolLaunchParams> parsed = ProtocolLaunchParams.parseLaunchUri(
                "fdc3-java-workbench://launch?webSocketUrl=ws%3A%2F%2Flocalhost%3A8090%2Ffdc3%2Fws&sharedSecret=secret");
        assertTrue(parsed.isPresent());
        assertEquals("ws://localhost:8090/fdc3/ws", parsed.get().getWebSocketUrl());
        assertEquals("secret", parsed.get().getSharedSecret());
    }

    @Test
    void redactsSharedSecret() {
        String redacted = ProtocolLaunchParams.redactLaunchUri(
                "fdc3-java-workbench://launch?webSocketUrl=ws://x&sharedSecret=top-secret");
        assertTrue(redacted.contains("sharedSecret=<redacted>"));
        assertTrue(!redacted.contains("top-secret"));
    }

    @Test
    void fromArgsRejoinsAmpersandSplit() {
        Optional<ProtocolLaunchParams> parsed = ProtocolLaunchParams.fromArgs(new String[] {
                "fdc3-java-workbench://launch?webSocketUrl=ws://localhost:8090/fdc3/ws",
                "sharedSecret=abc"
        });
        assertTrue(parsed.isPresent());
        assertEquals("abc", parsed.get().getSharedSecret());
    }
}
