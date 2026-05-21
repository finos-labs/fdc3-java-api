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

package org.finos.fdc3.proxy.schema;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathNotFoundException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;

import io.github.robmoffat.support.MatchingUtils;
import io.github.robmoffat.support.RowFieldMatcher;
import io.github.robmoffat.world.PropsWorld;

/**
 * Registers the {@code matches_type} table column matcher for FDC3 DACP messages.
 * Java equivalent of {@code @finos/fdc3-schema/test/fdc3SchemaMatchers.ts}.
 */
public final class Fdc3SchemaMatchers {

    private static final String MATCHES_TYPE_SUFFIX = "matches_type";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final RowFieldMatcher MATCHES_TYPE_MATCHER = new RowFieldMatcher() {
        @Override
        public boolean matchesField(String field) {
            return field.endsWith(MATCHES_TYPE_SUFFIX);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean matchField(PropsWorld world, String field, String schemaId, Object rowData) {
            String path = MatchingUtils.pathForFieldSuffix(field, MATCHES_TYPE_SUFFIX);
            if (path == null) {
                return false;
            }

            Object value = valueAtPath(rowData, path);

            Map<String, JsonSchema> schemas = (Map<String, JsonSchema>) world.get("schemas");
            if (schemas == null) {
                world.log("Schemas not loaded — call Given schemas loaded first");
                return false;
            }

            JsonSchema schema = schemas.get(schemaId);
            if (schema == null) {
                throw new IllegalStateException("No schema found for " + schemaId);
            }

            try {
                JsonNode jsonValue = OBJECT_MAPPER.valueToTree(value);
                Set<ValidationMessage> errors = schema.validate(jsonValue);
                if (errors.isEmpty()) {
                    return true;
                }
                String messages = errors.stream()
                        .map(ValidationMessage::getMessage)
                        .collect(Collectors.joining("; "));
                world.log("Schema validation failed for " + schemaId + ": " + messages);
                return false;
            } catch (Exception e) {
                world.log("Schema validation error for " + schemaId + ": " + e.getMessage());
                return false;
            }
        }
    };

    static {
        MatchingUtils.registerFieldMatcher(MATCHES_TYPE_MATCHER);
    }

    private Fdc3SchemaMatchers() {
    }

    public static void registerFdc3SchemaMatchers() {
        // Matcher registered in static initializer (same pattern as TypeScript side-effect import).
    }

    private static Object valueAtPath(Object data, String path) {
        if (path.isEmpty()) {
            return data;
        }
        try {
            JXPathContext context = JXPathContext.newContext(data);
            context.setLenient(true);
            String xpathName = "/" + path.replaceAll("\\.", "/");
            xpathName = xpathName.replaceAll("(/[^/]+)/length$", "count($1)");
            return context.getValue(xpathName);
        } catch (JXPathNotFoundException e) {
            return null;
        }
    }
}
