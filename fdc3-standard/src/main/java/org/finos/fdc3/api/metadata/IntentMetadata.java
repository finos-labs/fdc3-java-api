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

package org.finos.fdc3.api.metadata;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Metadata describing an Intent.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IntentMetadata {

    private String name;
    private String displayName;

    /**
     * Default constructor for Jackson deserialization.
     */
    public IntentMetadata() {
    }

    public IntentMetadata(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    /**
     * The unique name of the intent that can be invoked by the raiseIntent call.
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Display name for the intent.
     */
    @JsonProperty("displayName")
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        IntentMetadata that = (IntentMetadata) other;
        return Objects.equals(name, that.name) && Objects.equals(displayName, that.displayName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, displayName);
    }

    @Override
    public String toString() {
        return "IntentMetadata{name=" + name + ", displayName=" + displayName + "}";
    }
}
