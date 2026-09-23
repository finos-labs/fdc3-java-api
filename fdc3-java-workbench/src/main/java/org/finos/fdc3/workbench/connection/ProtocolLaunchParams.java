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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Parses WSCP launch parameters from a custom protocol handler URL.
 * <p>
 * Example:
 * {@code fdc3-java-workbench://launch?webSocketUrl=ws%3A%2F%2Flocalhost%3A8090%2Ffdc3%2Fws&sharedSecret=...}
 */
public final class ProtocolLaunchParams {

    public static final String SCHEME = "fdc3-java-workbench";

    private static final Pattern SHARED_SECRET_VALUE =
            Pattern.compile("(?i)(sharedSecret=)[^&#\\s]*");

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

    public static Optional<ProtocolLaunchParams> fromArgs(String[] args) {
        if (args == null || args.length == 0) {
            return Optional.empty();
        }
        for (String arg : args) {
            Optional<ProtocolLaunchParams> parsed = parseLaunchUri(arg);
            if (parsed.isPresent()) {
                return parsed;
            }
        }
        for (int i = 0; i < args.length; i++) {
            if (!isSchemeUrl(args[i])) {
                continue;
            }
            StringBuilder rebuilt = new StringBuilder(args[i].trim());
            for (int j = i + 1; j < args.length; j++) {
                String part = args[j];
                if (part == null || part.isBlank()) {
                    continue;
                }
                if (isSchemeUrl(part)) {
                    break;
                }
                if (!rebuilt.toString().contains("?")) {
                    rebuilt.append('?');
                }
                if (!part.startsWith("&")) {
                    rebuilt.append('&');
                }
                rebuilt.append(part);
            }
            Optional<ProtocolLaunchParams> parsed = parseLaunchUri(rebuilt.toString());
            if (parsed.isPresent()) {
                return parsed;
            }
        }
        return Optional.empty();
    }

    public static String redactLaunchUri(String uri) {
        if (uri == null) {
            return null;
        }
        return SHARED_SECRET_VALUE.matcher(uri).replaceAll("$1<redacted>");
    }

    private static boolean isSchemeUrl(String value) {
        return value != null
                && value.regionMatches(true, 0, SCHEME + "://", 0, SCHEME.length() + 3);
    }

    public static Optional<ProtocolLaunchParams> parseLaunchUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return Optional.empty();
        }

        String normalized = uri.trim();
        if (!normalized.regionMatches(true, 0, SCHEME + "://", 0, SCHEME.length() + 3)) {
            return Optional.empty();
        }

        try {
            int queryStart = normalized.indexOf('?');
            String rawQuery = queryStart >= 0 ? normalized.substring(queryStart + 1) : null;
            if (rawQuery != null) {
                int hash = rawQuery.indexOf('#');
                if (hash >= 0) {
                    rawQuery = rawQuery.substring(0, hash);
                }
            }
            Map<String, String> query = parseQuery(rawQuery);
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
