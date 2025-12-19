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

package org.finos.fdc3.proxy.support;

import java.util.HashMap;
import java.util.Map;

import org.finos.fdc3.api.context.Context;

/**
 * Pre-defined context objects for testing.
 */
public final class ContextMap {

    private static final Map<String, Context> CONTEXTS = new HashMap<>();

    static {
        // fdc3.instrument
        Map<String, Object> instrumentId = new HashMap<>();
        instrumentId.put("ticker", "AAPL");
        Context instrument = new Context("fdc3.instrument", "Apple", instrumentId);
        CONTEXTS.put("fdc3.instrument", instrument);

        // fdc3.country
        Map<String, Object> countryId = new HashMap<>();
        countryId.put("COUNTRY_ISOALPHA2", "SE");
        countryId.put("COUNTRY_ISOALPHA3", "SWE");
        Context country = new Context("fdc3.country", "Sweden", countryId);
        CONTEXTS.put("fdc3.country", country);

        // fdc3.unsupported
        Context unsupported = new Context("fdc3.unsupported");
        unsupported.put("bogus", true);
        CONTEXTS.put("fdc3.unsupported", unsupported);

        // fdc3.cancel-me
        Context cancelMe = new Context("fdc3.cancel-me");
        CONTEXTS.put("fdc3.cancel-me", cancelMe);
    }

    private ContextMap() {
        // Utility class
    }

    public static Context get(String type) {
        return CONTEXTS.get(type);
    }

    public static boolean contains(String type) {
        return CONTEXTS.containsKey(type);
    }
}

