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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Channels may be visualized and selectable by users. DisplayMetadata may be used to
 * provide hints on how to see them.
 * For App channels, displayMetadata would typically not be present.
 *
 * A system channel will be global enough to have a presence across many apps. This gives us
 * some hints to render them in a standard way. It is assumed it may have other properties too,
 * but if it has these, this is their meaning.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DisplayMetadata {

    private String name;
    private String color;
    private String glyph;

    /**
     * Default constructor for Jackson deserialization.
     */
    public DisplayMetadata() {
    }

    public DisplayMetadata(String name, String color, String glyph) {
        this.name = name;
        this.color = color;
        this.glyph = glyph;
    }

    /**
     * A user-readable name for this channel, e.g: `"Red"`.
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The color that should be associated within this channel when displaying this channel in a
     * UI, e.g: `0xFF0000`.
     */
    @JsonProperty("color")
    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    /**
     * A URL of an image that can be used to display this channel.
     */
    @JsonProperty("glyph")
    public String getGlyph() {
        return glyph;
    }

    public void setGlyph(String glyph) {
        this.glyph = glyph;
    }

    /**
     * Create a DisplayMetadata from a Map.
     */
    public static DisplayMetadata fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        String name = (String) map.get("name");
        String color = (String) map.get("color");
        String glyph = (String) map.get("glyph");
        return new DisplayMetadata(name, color, glyph);
    }
}
