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

import java.util.Collections;
import java.util.Map;

public class ContextContants {


    public static Map<String, Map<String, Boolean>> CONTEXT_ID_KEYS_MAPPING = Map.of(
            Contact.TYPE, Contact.VALID_ID_KEYS,
            Country.TYPE, Country.VALID_ID_KEYS,
            Currency.TYPE, Currency.VALID_ID_KEYS,
            Instrument.TYPE, Instrument.VALID_ID_KEYS,
            Organization.TYPE, Organization.VALID_ID_KEYS,
            ContactList.TYPE, Collections.emptyMap(),
            InstrumentList.TYPE, Collections.emptyMap()
    );

}
