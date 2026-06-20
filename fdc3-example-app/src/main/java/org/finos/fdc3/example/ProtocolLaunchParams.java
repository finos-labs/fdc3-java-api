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

package org.finos.fdc3.example;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Parses WSCP launch parameters from a custom protocol handler URL.
 * <p>
 * Example:
 * {@code fdc3-java-app://launch?webSocketUrl=ws%3A%2F%2Flocalhost%3A8090%2Ffdc3%2Fws&sharedSecret=...}
 */
public final class ProtocolLaunchParams {

    /** Custom URL scheme registered with the OS for this application. */
    public static final String SCHEME = "fdc3-java-app";

    private final String webSocketUrl;
    private final String sharedSecret;

    private ProtocolLaunchParams(String webSocketUrl, String sharedSecret) {
        this.webSocketUrl = webSocketUrl;
        this.sharedSecret = sharedSecret;
    }

    public String getWebSocketUrl() {
        return webSocketUrl;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    /**
     * Looks for a protocol launch URL in the process arguments.
     */
    public static Optional<ProtocolLaunchParams> fromArgs(String[] args) {
        if (args == null) {
            return Optional.empty();
        }
        for (String arg : args) {
            Optional<ProtocolLaunchParams> parsed = parseLaunchUri(arg);
            if (parsed.isPresent()) {
                return parsed;
            }
        }
        return Optional.empty();
    }

    /**
     * Parses a {@code fdc3-java-app://...} launch URI.
     */
    public static Optional<ProtocolLaunchParams> parseLaunchUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return Optional.empty();
        }

        String normalized = uri.trim();
        if (!normalized.regionMatches(true, 0, SCHEME + "://", 0, SCHEME.length() + 3)) {
            return Optional.empty();
        }

        try {
            URI parsed = URI.create(normalized);
            Map<String, String> query = parseQuery(parsed.getRawQuery());
            String webSocketUrl = query.get("webSocketUrl");
            String sharedSecret = query.get("sharedSecret");
            if (webSocketUrl == null || webSocketUrl.isBlank()
                    || sharedSecret == null || sharedSecret.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new ProtocolLaunchParams(webSocketUrl, sharedSecret));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return params;
        }
        for (String pair : rawQuery.split("&")) {
            int separator = pair.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }
}
