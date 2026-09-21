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

package org.finos.fdc3.api.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

/**
 * Identifies an application, or instance of an application, and is used to target FDC3 API calls,
 * such as `fdc3.open` or `fdc3.raiseIntent` at specific applications or application instances.
 *
 * Will always include at least an `appId` field, which uniquely identifies a specific app.
 *
 * If the `instanceId` field is set then the `AppIdentifier` object represents a specific instance
 * of the application that may be addressed using that Id.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppIdentifier {

    private String appID;
    private String instanceID;
    private String desktopAgent;

    /**
     * Default constructor for Jackson deserialization.
     */
    public AppIdentifier() {
    }

    @JsonCreator
    public AppIdentifier(
            @JsonProperty("appId") String appID,
            @JsonProperty("instanceId") String instanceID,
            @JsonProperty("desktopAgent") String desktopAgent) {
        this.appID = appID;
        this.instanceID = instanceID;
        this.desktopAgent = desktopAgent;
    }

    public AppIdentifier(String appID, String instanceID) {
        this(appID, instanceID, null);
    }

    public AppIdentifier(String appID) {
        this(appID, null, null);
    }

    /**
     * The unique application identifier located within a specific application directory instance.
     * An example of an appId might be 'app@sub.root'.
     */
    @JsonProperty("appId")
    public String getAppId() {
        return appID;
    }

    public void setAppId(String appID) {
        this.appID = appID;
    }

    /**
     * An optional instance identifier, indicating that this object represents a specific instance
     * of the application described.
     */
    @JsonProperty("instanceId")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getInstanceId() {
        return instanceID;
    }

    public void setInstanceId(String instanceID) {
        this.instanceID = instanceID;
    }

    /**
     * Identifier of the desktop agent, used in bridging.
     */
    @JsonProperty("desktopAgent")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDesktopAgent() {
        return desktopAgent;
    }

    public void setDesktopAgent(String desktopAgent) {
        this.desktopAgent = desktopAgent;
    }

    /**
     * Creates an AppIdentifier from a Map representation.
     *
     * @param map the map containing appId, instanceId, and/or desktopAgent keys
     * @return a new AppIdentifier instance, or null if the map is null
     */
    public static AppIdentifier fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        
        String appID = (String) map.get("appId");
        String instanceID = (String) map.get("instanceId");
        String desktopAgent = (String) map.get("desktopAgent");
        
        return new AppIdentifier(appID, instanceID, desktopAgent);
    }

    /**
     * Compares the identity fields only: {@code appId}, {@code instanceId} and
     * {@code desktopAgent}.
     * <p>
     * Deliberately accepts any {@code AppIdentifier}, including an {@link
     * org.finos.fdc3.api.metadata.AppMetadata}, rather than requiring an exact class match.
     * {@code findInstances} returns app metadata upcast to {@code AppIdentifier}, so requiring
     * an exact match would mean a plain identifier could never be found in its result. The
     * descriptive fields {@code AppMetadata} adds come from an app directory and describe an
     * app rather than identifying it, which is why subclasses do not refine this comparison.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AppIdentifier)) {
            return false;
        }
        AppIdentifier that = (AppIdentifier) other;
        return Objects.equals(appID, that.appID)
                && Objects.equals(instanceID, that.instanceID)
                && Objects.equals(desktopAgent, that.desktopAgent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appID, instanceID, desktopAgent);
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder("AppIdentifier{appId=").append(appID);
        if (instanceID != null) {
            text.append(", instanceId=").append(instanceID);
        }
        if (desktopAgent != null) {
            text.append(", desktopAgent=").append(desktopAgent);
        }
        return text.append('}').toString();
    }
}
