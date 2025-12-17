/**
 * Copyright 2023 Wellington Management Company LLP
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

package org.finos.fdc3.api.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Metadata relating to the FDC3 Desktop Agent implementation and its provider.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImplementationMetadata {

    private String fdc3Version;
    private String provider;
    private String providerVersion;
    private AppMetadata appMetadata;
    private OptionalFeatures optionalFeatures;

    /**
     * Default constructor for Jackson deserialization.
     */
    public ImplementationMetadata() {
    }

    /**
     * The version number of the FDC3 specification that the implementation provides.
     * The string must be a numeric semver version, e.g. 1.2 or 1.2.1.
     */
    @JsonProperty("fdc3Version")
    public String getFdc3Version() {
        return fdc3Version;
    }

    public void setFdc3Version(String fdc3Version) {
        this.fdc3Version = fdc3Version;
    }

    /**
     * The name of the provider of the Desktop Agent implementation (e.g. Finsemble, Glue42, OpenFin etc.).
     */
    @JsonProperty("provider")
    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * The version of the provider of the Desktop Agent implementation (e.g. 5.3.0).
     */
    @JsonProperty("providerVersion")
    public String getProviderVersion() {
        return providerVersion;
    }

    public void setProviderVersion(String providerVersion) {
        this.providerVersion = providerVersion;
    }

    /**
     * The calling application instance's own metadata, according to the Desktop Agent (MUST include at least the `appId` and `instanceId`).
     */
    @JsonProperty("appMetadata")
    public AppMetadata getAppMetadata() {
        return appMetadata;
    }

    public void setAppMetadata(AppMetadata appMetadata) {
        this.appMetadata = appMetadata;
    }

    /**
     * Metadata indicating whether the Desktop Agent implements optional features of
     * the Desktop Agent API.
     */
    @JsonProperty("optionalFeatures")
    public OptionalFeatures getOptionalFeatures() {
        return optionalFeatures;
    }

    public void setOptionalFeatures(OptionalFeatures optionalFeatures) {
        this.optionalFeatures = optionalFeatures;
    }

    /**
     * Metadata indicating whether the Desktop Agent implements optional features of
     * the Desktop Agent API.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OptionalFeatures {

        private boolean originatingAppMetadata;
        private boolean userChannelMembershipAPIs;
        private boolean desktopAgentBridging;

        /**
         * Default constructor for Jackson deserialization.
         */
        public OptionalFeatures() {
        }

        /**
         * Used to indicate whether the exposure of 'originating app metadata' for
         * context and intent messages is supported by the Desktop Agent.
         */
        @JsonProperty("OriginatingAppMetadata")
        public boolean isOriginatingAppMetadata() {
            return originatingAppMetadata;
        }

        public void setOriginatingAppMetadata(boolean originatingAppMetadata) {
            this.originatingAppMetadata = originatingAppMetadata;
        }

        /**
         * Used to indicate whether the optional `fdc3.joinUserChannel`,
         * `fdc3.getCurrentChannel` and `fdc3.leaveCurrentChannel` are implemented by
         * the Desktop Agent.
         */
        @JsonProperty("UserChannelMembershipAPIs")
        public boolean isUserChannelMembershipAPIs() {
            return userChannelMembershipAPIs;
        }

        public void setUserChannelMembershipAPIs(boolean userChannelMembershipAPIs) {
            this.userChannelMembershipAPIs = userChannelMembershipAPIs;
        }

        /**
         * Used to indicate whether the experimental Desktop Agent Bridging
         * feature is implemented by the Desktop Agent.
         */
        @JsonProperty("DesktopAgentBridging")
        public boolean isDesktopAgentBridging() {
            return desktopAgentBridging;
        }

        public void setDesktopAgentBridging(boolean desktopAgentBridging) {
            this.desktopAgentBridging = desktopAgentBridging;
        }
    }
}
