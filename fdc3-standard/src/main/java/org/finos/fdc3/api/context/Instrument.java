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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;

@JsonPropertyOrder({
        "type",
        "name",
        "id"
})
public class Instrument  extends Context {
    public static String TYPE = "fdc3.instrument";
    public static Map<String, Boolean> VALID_ID_KEYS = Map.of(
            "FDS_ID", false,
            "PERMID", false,
            "RIC", false,
            "BBG", false,
            "ticker", false,
            "FIGI", false,
            "ISIN", false,
            "CUSIP", false,
            "SEDOL", false);

    public Instrument() {
        super(TYPE);
    }
}
