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
 * Describes an image file, typically a screenshot, that often represents the application in
 * a common usage scenario.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Image {

    private String src;
    private String size;
    private String type;
    private String label;

    /**
     * Default constructor for Jackson deserialization.
     */
    public Image() {
    }

    public Image(String src, String size, String type, String label) {
        this.src = src;
        this.size = size;
        this.type = type;
        this.label = label;
    }

    /**
     * The image url.
     */
    @JsonProperty("src")
    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    /**
     * The image dimension, formatted as `<height>x<width>`.
     */
    @JsonProperty("size")
    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    /**
     * Image media type. If not present the Desktop Agent may use the src file extension.
     */
    @JsonProperty("type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * Caption for the image.
     */
    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
