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
 * This class contains all the configuration needed to establish a connection
 * to an FDC3 Desktop Agent over WebSocket using the Web Connection Protocol (WCP).
 */
public class GetAgentParams {

    private final String webSocketUrl;
    private final String identityUrl;
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
        this.identityUrl = builder.identityUrl;
        this.instanceId = builder.instanceId;
        this.instanceUuid = builder.instanceUuid;
        this.channelSelector = builder.channelSelector;
        this.intentResolver = builder.intentResolver;
        this.timeoutMs = builder.timeoutMs;
        this.messageExchangeTimeout = builder.messageExchangeTimeout;
        this.appLaunchTimeout = builder.appLaunchTimeout;
        this.heartbeatIntervalMs = builder.heartbeatIntervalMs;
    }

    /**
     * Gets the WebSocket URL to connect to the Desktop Agent.
     *
     * @return the WebSocket URL
     */
    public String getWebSocketUrl() {
        return webSocketUrl;
    }

    /**
     * Gets the identity URL used to identify this application.
     * This URL is sent to the Desktop Agent during the handshake.
     *
     * @return the identity URL
     */
    public String getIdentityUrl() {
        return identityUrl;
    }

    /**
     * Gets the instance ID used to identify this application instance.
     * This is sent to the Desktop Agent during the handshake.
     *
     * @return the instance ID
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Gets the instance UUID used as a shared secret with the Desktop Agent.
     * This is used to validate the application's identity during reconnection.
     *
     * @return the instance UUID
     */
    public String getInstanceUuid() {
        return instanceUuid;
    }

    /**
     * Gets the channel selector implementation for user channel selection UI.
     *
     * @return the channel selector
     */
    public ChannelSelector getChannelSelector() {
        return channelSelector;
    }

    /**
     * Gets the intent resolver implementation for intent resolution UI.
     *
     * @return the intent resolver
     */
    public IntentResolver getIntentResolver() {
        return intentResolver;
    }

    /**
     * Gets the connection timeout in milliseconds.
     *
     * @return the timeout in milliseconds
     */
    public long getTimeoutMs() {
        return timeoutMs;
    }

    /**
     * Gets the message exchange timeout in milliseconds.
     * This is used for API calls to the Desktop Agent.
     *
     * @return the message exchange timeout
     */
    public long getMessageExchangeTimeout() {
        return messageExchangeTimeout;
    }

    /**
     * Gets the app launch timeout in milliseconds.
     * This is used when opening apps or raising intents.
     *
     * @return the app launch timeout
     */
    public long getAppLaunchTimeout() {
        return appLaunchTimeout;
    }

    /**
     * Gets the heartbeat interval in milliseconds.
     *
     * @return the heartbeat interval
     */
    public long getHeartbeatIntervalMs() {
        return heartbeatIntervalMs;
    }

    /**
     * Creates a new builder for GetAgentParams.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for GetAgentParams.
     */
    public static class Builder {
        private String webSocketUrl;
        private String identityUrl;
        private String instanceId;
        private String instanceUuid;
        private ChannelSelector channelSelector = new DefaultChannelSelector();
        private IntentResolver intentResolver = new DefaultIntentResolver();
        private long timeoutMs = 10000;
        private long messageExchangeTimeout = 10000;
        private long appLaunchTimeout = 30000;
        private long heartbeatIntervalMs = 5000;

        /**
         * Sets the WebSocket URL to connect to the Desktop Agent.
         *
         * @param webSocketUrl the WebSocket URL (required)
         * @return this builder
         */
        public Builder webSocketUrl(String webSocketUrl) {
            this.webSocketUrl = webSocketUrl;
            return this;
        }

        /**
         * Sets the identity URL used to identify this application.
         *
         * @param identityUrl the identity URL (required)
         * @return this builder
         */
        public Builder identityUrl(String identityUrl) {
            this.identityUrl = identityUrl;
            return this;
        }

        /**
         * Sets the instance ID used to identify this application instance.
         *
         * @param instanceId the instance ID (required)
         * @return this builder
         */
        public Builder instanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        /**
         * Sets the instance UUID used as a shared secret with the Desktop Agent.
         *
         * @param instanceUuid the instance UUID (required)
         * @return this builder
         */
        public Builder instanceUuid(String instanceUuid) {
            this.instanceUuid = instanceUuid;
            return this;
        }

        /**
         * Sets the channel selector implementation.
         *
         * @param channelSelector the channel selector (default: NullChannelSelector)
         * @return this builder
         */
        public Builder channelSelector(ChannelSelector channelSelector) {
            this.channelSelector = channelSelector != null ? channelSelector : new DefaultChannelSelector();
            return this;
        }

        /**
         * Sets the intent resolver implementation.
         *
         * @param intentResolver the intent resolver (default: NullIntentResolver)
         * @return this builder
         */
        public Builder intentResolver(IntentResolver intentResolver) {
            this.intentResolver = intentResolver != null ? intentResolver : new DefaultIntentResolver();
            return this;
        }

        /**
         * Sets the connection timeout in milliseconds.
         *
         * @param timeoutMs the timeout (default: 10000)
         * @return this builder
         */
        public Builder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        /**
         * Sets the message exchange timeout in milliseconds.
         *
         * @param messageExchangeTimeout the timeout (default: 10000)
         * @return this builder
         */
        public Builder messageExchangeTimeout(long messageExchangeTimeout) {
            this.messageExchangeTimeout = messageExchangeTimeout;
            return this;
        }

        /**
         * Sets the app launch timeout in milliseconds.
         *
         * @param appLaunchTimeout the timeout (default: 30000)
         * @return this builder
         */
        public Builder appLaunchTimeout(long appLaunchTimeout) {
            this.appLaunchTimeout = appLaunchTimeout;
            return this;
        }

        /**
         * Sets the heartbeat interval in milliseconds.
         *
         * @param heartbeatIntervalMs the interval (default: 5000)
         * @return this builder
         */
        public Builder heartbeatIntervalMs(long heartbeatIntervalMs) {
            this.heartbeatIntervalMs = heartbeatIntervalMs;
            return this;
        }

        /**
         * Builds the GetAgentParams instance.
         *
         * @return the built GetAgentParams
         * @throws IllegalArgumentException if required parameters are missing
         */
        public GetAgentParams build() {
            if (webSocketUrl == null || webSocketUrl.isEmpty()) {
                throw new IllegalArgumentException("webSocketUrl is required");
            }
            if (identityUrl == null || identityUrl.isEmpty()) {
                throw new IllegalArgumentException("identityUrl is required");
            }
            if (instanceId == null || instanceId.isEmpty()) {
                throw new IllegalArgumentException("instanceId is required");
            }
            if (instanceUuid == null || instanceUuid.isEmpty()) {
                throw new IllegalArgumentException("instanceUuid is required");
            }
            return new GetAgentParams(this);
        }
    }
}
