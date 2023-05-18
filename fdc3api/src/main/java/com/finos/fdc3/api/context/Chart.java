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

package com.finos.fdc3.api.context;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Set;

@JsonPropertyOrder({
        "type",
        "instruments",
        "range",
        "style",
        "otherConfig"
})
public class Chart extends Context {
    public static String TYPE = "fdc3.chart";
    public static Set<String> STYLE = Set.of("line", "bar", "stacked-bar", "mountain", "candle", "pie", "scatter", "histogram", "heatmap", "custom");

    @JsonProperty("instruments")
    private List<Instrument> instruments;

    @JsonProperty("range")
    private TimeRange range;

    @JsonProperty("style")
    private String style;

    @JsonProperty("otherConfig")
    private Object otherConfig;

    public Chart(List<Instrument> instruments) {
        super(TYPE);
        this.instruments = instruments;
    }

    public List<Instrument> getInstruments() {
        return instruments;
    }

    public void setInstruments(List<Instrument> instruments) {
        this.instruments = instruments;
    }

    public TimeRange getRange() {
        return range;
    }

    public void setRange(TimeRange range) {
        this.range = range;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        if(!STYLE.contains(style)) {
            throw new IllegalArgumentException("Invalid Style argument!");
        }
        this.style = style;
    }

    public Object getOtherConfig() {
        return otherConfig;
    }

    public void setOtherConfig(Object otherConfig) {
        this.otherConfig = otherConfig;
    }
}
