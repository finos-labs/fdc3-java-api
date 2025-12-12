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
import org.joda.time.DateTime;

@JsonPropertyOrder({
        "type",
        "value",
        "price",
        "CURRENCY_ISOCODE",
        "valuationTime",
        "expiryTime"
})
public class Valuation extends Context {
    public static String TYPE = "fdc3.valuation";

    @JsonProperty("value")
    private Double value;

    @JsonProperty("price")
    private Double price;

    @JsonProperty("CURRENCY_ISOCODE")
    private String currencyIsocode;

    @JsonProperty("valuationTime")
    private DateTime valuationTime;

    @JsonProperty("expiryTime")
    private DateTime expiryTime;

    public Valuation(Double value, String currencyIsocode) {
        super(TYPE);
        this.value = value;
        this.currencyIsocode = currencyIsocode;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getCurrencyIsocode() {
        return currencyIsocode;
    }

    public void setCurrencyIsocode(String currencyIsocode) {
        this.currencyIsocode = currencyIsocode;
    }

    public DateTime getValuationTime() {
        return valuationTime;
    }

    public void setValuationTime(DateTime valuationTime) {
        this.valuationTime = valuationTime;
    }

    public DateTime getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(DateTime expiryTime) {
        this.expiryTime = expiryTime;
    }
}
