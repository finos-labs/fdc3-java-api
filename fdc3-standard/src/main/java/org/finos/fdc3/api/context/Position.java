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

import java.math.BigDecimal;

@JsonPropertyOrder({
        "type",
        "id",
        "name",
        "holding",
        "instrument"
})
public class Position extends Context {
    public static String TYPE = "fdc3.position";

    @JsonProperty("holding")
    private BigDecimal holding;

    @JsonProperty("instrument")
    private Instrument instrument;

    public Position(BigDecimal holding, Instrument instrument) {
        super(TYPE);
        this.holding = holding;
        this.instrument = instrument;
    }

    public BigDecimal getHolding() {
        return holding;
    }

    public void setHolding(BigDecimal holding) {
        this.holding = holding;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }
}