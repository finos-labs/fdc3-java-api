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

package org.finos.fdc3.getagent;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.finos.fdc3.api.ui.ChannelSelector;
import org.finos.fdc3.api.ui.IntentResolver;
import org.finos.fdc3.getagent.ui.DefaultChannelSelector;
import org.finos.fdc3.getagent.ui.DefaultIntentResolver;
import org.finos.fdc3.proxy.util.Logger;

/**
 * Parameters for obtaining a DesktopAgent connection via WebSocket.
 * <p>
 * Uses the WebSocket Connection Protocol (WSCP) for the initial handshake.
 * <p>
 * The following environment variables / system properties provide defaults:
 * <ul>
 *   <li>{@code FDC3_WEBSOCKET_URL} — deployment WebSocket endpoint (e.g. ws://host/fdc3/ws)</li>
 *   <li>{@code FDC3_CONNECTION_SECRET} — pairing secret from the Desktop Agent UI (required)</li>
 * </ul>
 * The shared secret implicitly identifies the FDC3 session and app instance. The same secret
 * MUST be presented again when reconnecting after interruption.
 */
public class GetAgentParams {

    public static final String PROP_WEBSOCKET_URL = "FDC3_WEBSOCKET_URL";
    public static final String PROP_CONNECTION_SECRET = "FDC3_CONNECTION_SECRET";

    private final String webSocketUrl;
    private final String sharedSecret;
    private final ChannelSelector channelSelector;
    private final IntentResolver intentResolver;
    private final long timeoutMs;
    private final long messageExchangeTimeout;
    private final long appLaunchTimeout;
    private final long heartbeatIntervalMs;
    private final boolean allowInsecureTransport;

    private GetAgentParams(Builder builder) {
        this.webSocketUrl = builder.webSocketUrl;
        this.sharedSecret = builder.sharedSecret;
        this.channelSelector = builder.channelSelector;
        this.intentResolver = builder.intentResolver;
        this.timeoutMs = builder.timeoutMs;
        this.messageExchangeTimeout = builder.messageExchangeTimeout;
        this.appLaunchTimeout = builder.appLaunchTimeout;
        this.heartbeatIntervalMs = builder.heartbeatIntervalMs;
        this.allowInsecureTransport = builder.allowInsecureTransport;
    }

    public String getWebSocketUrl() {
        return webSocketUrl;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public ChannelSelector getChannelSelector() {
        return channelSelector;
    }

    public IntentResolver getIntentResolver() {
        return intentResolver;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public long getMessageExchangeTimeout() {
        return messageExchangeTimeout;
    }

    public long getAppLaunchTimeout() {
        return appLaunchTimeout;
    }

    public long getHeartbeatIntervalMs() {
        return heartbeatIntervalMs;
    }

    /** Whether plaintext {@code ws://} to a non-loopback host was explicitly permitted. */
    public boolean isAllowInsecureTransport() {
        return allowInsecureTransport;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String webSocketUrl = firstNonEmpty(
                System.getenv(PROP_WEBSOCKET_URL),
                System.getProperty(PROP_WEBSOCKET_URL));
        private String sharedSecret = firstNonEmpty(
                System.getenv(PROP_CONNECTION_SECRET),
                System.getProperty(PROP_CONNECTION_SECRET));
        private ChannelSelector channelSelector = new DefaultChannelSelector();
        private IntentResolver intentResolver = new DefaultIntentResolver();
        private long timeoutMs = 10000;
        private long messageExchangeTimeout = 10000;
        private long appLaunchTimeout = 30000;
        private long heartbeatIntervalMs = 5000;
        private boolean allowInsecureTransport = false;

        private static String firstNonEmpty(String a, String b) {
            if (a != null && !a.isEmpty()) {
                return a;
            }
            if (b != null && !b.isEmpty()) {
                return b;
            }
            return null;
        }

        public Builder webSocketUrl(String webSocketUrl) {
            this.webSocketUrl = webSocketUrl;
            return this;
        }

        public Builder sharedSecret(String sharedSecret) {
            this.sharedSecret = sharedSecret;
            return this;
        }

        public Builder channelSelector(ChannelSelector channelSelector) {
            this.channelSelector = channelSelector != null ? channelSelector : new DefaultChannelSelector();
            return this;
        }

        public Builder intentResolver(IntentResolver intentResolver) {
            this.intentResolver = intentResolver != null ? intentResolver : new DefaultIntentResolver();
            return this;
        }

        public Builder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder messageExchangeTimeout(long messageExchangeTimeout) {
            this.messageExchangeTimeout = messageExchangeTimeout;
            return this;
        }

        public Builder appLaunchTimeout(long appLaunchTimeout) {
            this.appLaunchTimeout = appLaunchTimeout;
            return this;
        }

        public Builder heartbeatIntervalMs(long heartbeatIntervalMs) {
            this.heartbeatIntervalMs = heartbeatIntervalMs;
            return this;
        }

        /**
         * Permits a plaintext {@code ws://} URL to a non-loopback host.
         * <p>
         * The shared secret is sent in the first message of the connection, so without TLS it
         * is readable by anything on the network path. Loopback URLs are always allowed and do
         * not need this flag.
         *
         * @param allowInsecureTransport true to allow plaintext to a remote host
         */
        public Builder allowInsecureTransport(boolean allowInsecureTransport) {
            this.allowInsecureTransport = allowInsecureTransport;
            return this;
        }

        public GetAgentParams build() {
            if (webSocketUrl == null || webSocketUrl.isEmpty()) {
                throw new IllegalArgumentException("webSocketUrl is required");
            }
            if (sharedSecret == null || sharedSecret.isEmpty()) {
                throw new IllegalArgumentException("sharedSecret is required");
            }
            validateTransport(webSocketUrl, allowInsecureTransport);
            return new GetAgentParams(this);
        }
    }

    /**
     * Checks that the WebSocket URL is well formed and that the shared secret will not be sent
     * in clear text to a remote host.
     * <p>
     * The WSCP specification does not mandate transport security, so this is a hardening
     * decision of this implementation rather than a conformance requirement.
     */
    private static void validateTransport(String webSocketUrl, boolean allowInsecureTransport) {
        URI uri;
        try {
            uri = new URI(webSocketUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("webSocketUrl is not a valid URI: " + webSocketUrl, e);
        }

        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException(
                    "webSocketUrl must specify a scheme of ws or wss: " + webSocketUrl);
        }

        scheme = scheme.toLowerCase(Locale.ROOT);
        if ("wss".equals(scheme)) {
            return;
        }
        if (!"ws".equals(scheme)) {
            throw new IllegalArgumentException(
                    "webSocketUrl scheme must be ws or wss, but was " + scheme);
        }

        if (isLoopbackHost(uri.getHost())) {
            return;
        }

        if (!allowInsecureTransport) {
            throw new IllegalArgumentException(
                    "Refusing to send the shared secret in clear text to a remote host. Use a wss:// "
                            + "URL, or call allowInsecureTransport(true) to accept the risk: "
                            + webSocketUrl);
        }

        Logger.warn("Connecting over plaintext ws:// to {}. The shared secret will be sent "
                + "unencrypted and is readable on the network path.", uri.getHost());
    }

    /**
     * Whether a host refers to the local machine, in which case plaintext traffic does not
     * leave the host. Deliberately a literal comparison: resolving names here would make
     * validation depend on DNS.
     */
    private static boolean isLoopbackHost(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }

        String candidate = host.toLowerCase(Locale.ROOT);
        if (candidate.startsWith("[") && candidate.endsWith("]")) {
            candidate = candidate.substring(1, candidate.length() - 1);
        }

        return "localhost".equals(candidate)
                || "::1".equals(candidate)
                || "0:0:0:0:0:0:0:1".equals(candidate)
                || candidate.startsWith("127.");
    }
}
