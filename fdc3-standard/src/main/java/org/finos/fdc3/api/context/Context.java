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

package org.finos.fdc3.api.context;

import java.util.HashMap;
import java.util.Map;

import org.finos.fdc3.api.types.IntentResult;

/**
 * The base FDC3 Context type.
 * <p>
 * The {@code fdc3.context} type defines the basic contract, or "shape", for all data exchanged
 * by FDC3 operations. It is not usually used on its own: more specific type definitions,
 * standardized or custom, build on it to provide the structure and properties shared by all
 * FDC3 context data types.
 * <p>
 * This implementation extends {@link HashMap} rather than declaring fixed fields, which means
 * it:
 * <ul>
 *   <li>preserves every field through serialization and deserialization, including properties
 *       this version of the API does not know about, so no data is lost in transit;</li>
 *   <li>accepts any context type, including custom ones;</li>
 *   <li>can be used directly as a {@code Map}.</li>
 * </ul>
 * <p>
 * To work with a context as a typed object, use the converter in the {@code fdc3-context}
 * module, which maps between a {@code Context} and the generated type for its
 * {@code type} value.
 */
public class Context extends HashMap<String, Object> implements IntentResult {

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

    /**
     * Create a Context from a Map.
     * This is a convenience factory method for converting Map data to a Context object.
     *
     * @param map the map containing context data
     * @return a new Context containing all entries from the map
     */
    public static Context fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        Context context = new Context();
        context.putAll(map);
        return context;
    }
}
