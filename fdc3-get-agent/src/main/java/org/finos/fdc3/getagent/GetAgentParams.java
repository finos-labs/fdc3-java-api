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

import org.finos.fdc3.api.ui.ChannelSelector;
import org.finos.fdc3.api.ui.IntentResolver;
import org.finos.fdc3.getagent.ui.DefaultChannelSelector;
import org.finos.fdc3.getagent.ui.DefaultIntentResolver;

/**
 * Parameters for obtaining a DesktopAgent connection via WebSocket.
 * <p>
 * Uses the WebSocket Connection Protocol (WSCP) for the initial handshake.
 * <p>
 * The following environment variables / system properties provide defaults:
 * <ul>
 *   <li>{@code FDC3_WEBSOCKET_URL} — deployment WebSocket endpoint (e.g. ws://host/fdc3/ws)</li>
 *   <li>{@code FDC3_SESSION_ID} — DA user session identifier</li>
 *   <li>{@code FDC3_CONNECTION_SECRET} — pairing secret or launch token (required for initial connect only)</li>
 * </ul>
 */
public class GetAgentParams {

    public static final String PROP_WEBSOCKET_URL = "FDC3_WEBSOCKET_URL";
    public static final String PROP_SESSION_ID = "FDC3_SESSION_ID";
    public static final String PROP_CONNECTION_SECRET = "FDC3_CONNECTION_SECRET";

    private final String webSocketUrl;
    private final String sessionId;
    private final String sharedSecret;
    private final String appId;
    private final String instanceId;
    private final String instanceUuid;
    private final ChannelSelector channelSelector;
    private final IntentResolver intentResolver;
    private final long timeoutMs;
    private final long messageExchangeTimeout;
    private final long appLaunchTimeout;
    private final long heartbeatIntervalMs;

    private GetAgentParams(Builder builder) {
        this.webSocketUrl = builder.webSocketUrl;
        this.sessionId = builder.sessionId;
        this.sharedSecret = builder.sharedSecret;
        this.appId = builder.appId;
        this.instanceId = builder.instanceId;
        this.instanceUuid = builder.instanceUuid;
        this.channelSelector = builder.channelSelector;
        this.intentResolver = builder.intentResolver;
        this.timeoutMs = builder.timeoutMs;
        this.messageExchangeTimeout = builder.messageExchangeTimeout;
        this.appLaunchTimeout = builder.appLaunchTimeout;
        this.heartbeatIntervalMs = builder.heartbeatIntervalMs;
    }

    public String getWebSocketUrl() {
        return webSocketUrl;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public String getAppId() {
        return appId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getInstanceUuid() {
        return instanceUuid;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String webSocketUrl = firstNonEmpty(
                System.getenv(PROP_WEBSOCKET_URL),
                System.getProperty(PROP_WEBSOCKET_URL));
        private String sessionId = firstNonEmpty(
                System.getenv(PROP_SESSION_ID),
                System.getProperty(PROP_SESSION_ID));
        private String sharedSecret = firstNonEmpty(
                System.getenv(PROP_CONNECTION_SECRET),
                System.getProperty(PROP_CONNECTION_SECRET));
        private String appId = null;
        private String instanceId = null;
        private String instanceUuid = null;
        private ChannelSelector channelSelector = new DefaultChannelSelector();
        private IntentResolver intentResolver = new DefaultIntentResolver();
        private long timeoutMs = 10000;
        private long messageExchangeTimeout = 10000;
        private long appLaunchTimeout = 30000;
        private long heartbeatIntervalMs = 5000;

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

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder sharedSecret(String sharedSecret) {
            this.sharedSecret = sharedSecret;
            return this;
        }

        public Builder appId(String appId) {
            this.appId = appId;
            return this;
        }

        public Builder instanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder instanceUuid(String instanceUuid) {
            this.instanceUuid = instanceUuid;
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

        public GetAgentParams build() {
            if (webSocketUrl == null || webSocketUrl.isEmpty()) {
                throw new IllegalArgumentException("webSocketUrl is required");
            }
            if (sessionId == null || sessionId.isEmpty()) {
                throw new IllegalArgumentException("sessionId is required");
            }
            if ((sharedSecret == null || sharedSecret.isEmpty())
                    && (instanceUuid == null || instanceUuid.isEmpty())) {
                throw new IllegalArgumentException(
                        "sharedSecret is required for initial connection, or instanceUuid for reconnect");
            }
            return new GetAgentParams(this);
        }
    }
}
