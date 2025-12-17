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

import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.finos.fdc3.api.types.AppIdentifier;

/**
 * Extends an `AppIdentifier`, describing an application or instance of an application, with additional
 * descriptive metadata that is usually provided by an FDC3 App Directory that the desktop agent connects to.
 *
 * The additional information from an app directory can aid in rendering UI elements, such as a launcher
 * menu or resolver UI. This includes a title, description, tooltip and icon and screenshot URLs.
 *
 * Note that as `AppMetadata` instances are also `AppIdentifiers` they may be passed to the `app` argument
 * of `fdc3.open`, `fdc3.raiseIntent` etc.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppMetadata extends AppIdentifier {

    private String name;
    private String version;
    private Map<String, Object> instanceMetadata;
    private String title;
    private String tooltip;
    private String description;
    private Collection<Icon> icons;
    private Collection<Image> screenshots;
    private String resultType;

    /**
     * Default constructor for Jackson deserialization.
     */
    public AppMetadata() {
    }

    /**
     * The 'friendly' app name.
     * This field was used with the `open` and `raiseIntent` calls in FDC3 &lt;2.0, which now require an
     * `AppIdentifier` with `appId` set.
     * Note that for display purposes the `title` field should be used, if set, in preference to this field.
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** The Version of the application. */
    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * An optional set of, implementation specific, metadata fields that can be used to disambiguate instances,
     * such as a window title or screen position. Must only be set if `instanceId` is set.
     */
    @JsonProperty("instanceMetadata")
    public Map<String, Object> getInstanceMetadata() {
        return instanceMetadata;
    }

    public void setInstanceMetadata(Map<String, Object> instanceMetadata) {
        this.instanceMetadata = instanceMetadata;
    }

    /** A more user-friendly application title that can be used to render UI elements. */
    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /** A tooltip for the application that can be used to render UI elements. */
    @JsonProperty("tooltip")
    public String getTooltip() {
        return tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    /** A longer, multi-paragraph description for the application that could include markup. */
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** A list of icon URLs for the application that can be used to render UI elements. */
    @JsonProperty("icons")
    public Collection<Icon> getIcons() {
        return icons;
    }

    public void setIcons(Collection<Icon> icons) {
        this.icons = icons;
    }

    /** Images representing the app in common usage scenarios that can be used to render UI elements. */
    @JsonProperty("screenshots")
    public Collection<Image> getScreenshots() {
        return screenshots;
    }

    public void setScreenshots(Collection<Image> screenshots) {
        this.screenshots = screenshots;
    }

    /**
     * The type of output returned for any intent specified during resolution. May express a particular
     * context type (e.g. "fdc3.instrument"), channel (e.g. "channel") or a channel that will receive
     * a specified type (e.g. "channel&lt;fdc3.instrument&gt;").
     */
    @JsonProperty("resultType")
    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }
}
