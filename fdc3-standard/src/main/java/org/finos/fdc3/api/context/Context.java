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

package org.finos.fdc3.api.context;

import java.util.HashMap;
import java.util.Map;

/**
 * The base FDC3 Context type.
 * 
 * This implementation extends HashMap to store all properties, which allows it to:
 * - Preserve all fields during serialization/deserialization (no data loss)
 * - Support any context type, including custom ones
 * - Be used directly as a Map
 * - Be converted to typed context classes using the {@link #as(Class)} method
 * 
 * The `fdc3.context` type defines the basic contract or "shape" for all data exchanged by
 * FDC3 operations. As such, it is not really meant to be used on its own, but is imported
 * by more specific type definitions (standardized or custom) to provide the structure and
 * properties shared by all FDC3 context data types.
 */
public class Context extends HashMap<String, Object> {

    public Context() {
    }

    public Context(String type) {
        setType(type);
    }

    public Context(String type, String name) {
        setType(type);
        setName(name);
    }

    public Context(String type, String name, Map<String, Object> id) {
        setType(type);
        setName(name);
        setId(id);
    }

    /**
     * The type property is the only required part of the FDC3 context data schema.
     * The FDC3 API relies on the `type` property being present to route shared context data appropriately.
     */
    public String getType() {
        return (String) get("type");
    }

    public void setType(String type) {
        put("type", type);
    }

    /**
     * Context data objects may include a name property that can be used for more information,
     * or display purposes.
     */
    public String getName() {
        return (String) get("name");
    }

    public void setName(String name) {
        if (name != null) {
            put("name", name);
        } else {
            remove("name");
        }
    }

    /**
     * Context data objects may include a set of equivalent key-value pairs that can be used to
     * help applications identify and look up the context type they receive in their own domain.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getId() {
        return (Map<String, Object>) get("id");
    }

    public void setId(Map<String, Object> id) {
        if (id != null) {
            put("id", id);
        } else {
            remove("id");
        }
    }

}
