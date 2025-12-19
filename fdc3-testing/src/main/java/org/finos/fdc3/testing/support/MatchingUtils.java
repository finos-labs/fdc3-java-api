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

package org.finos.fdc3.testing.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fdc3.schema.SchemaConverter;
import org.finos.fdc3.testing.world.PropsWorld;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathNotFoundException;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;

import io.cucumber.datatable.DataTable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Utility class for matching and resolving test data.
 * This is equivalent to the TypeScript matching.ts module.
 */
public final class MatchingUtils {

    private static final SchemaConverter converter = new SchemaConverter();
    private static final ObjectMapper objectMapper = converter.getObjectMapper();

    private MatchingUtils() {
        // Utility class
    }

    private static Object extractFromWorld(Object world, String expression) {
        // Use JXPath to resolve the value from props
        try {
            JXPathContext context = JXPathContext.newContext(world);
            context.setLenient(true);
            String xpathName = "/" + expression.replaceAll("\\.", "/");
            // Convert .length to count(/path) for XPath
            xpathName = xpathName.replaceAll("(/[^/]+)/length$", "count($1)");
            // Convert 0-based array indexes to 1-based for JXPath (e.g., [0] -> [1])
            Matcher matcher = Pattern.compile("\\[(\\d+)\\]").matcher(xpathName);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                int index = Integer.parseInt(matcher.group(1));
                matcher.appendReplacement(sb, "[" + (index + 1) + "]");
            }
            matcher.appendTail(sb);
            xpathName = sb.toString();
            Object result = context.getValue(xpathName);
            // Unwrap Optional if needed
            if (result instanceof java.util.Optional) {
                result = ((java.util.Optional<?>) result).orElse(null);
            }
            // Convert numbers to rounded strings with 0 decimal places
            if (result instanceof Number) {
                return String.valueOf(Math.round(((Number) result).doubleValue()));
            }
            return result;
        } catch (JXPathNotFoundException e) {
            return null;
        }
    }
    
    /**
     * Resolve a field reference to its actual value.
     * <p>
     * If the name is wrapped in braces like {field.path}, it will be resolved
     * using JSONPath against the PropsWorld props. Special values like {null},
     * {true}, {false}, and numeric values are handled specially.
     *
     * @param name the field reference or literal value
     * @param world the PropsWorld containing test state
     * @return the resolved value
     */
    public static Object handleResolve(String name, PropsWorld world) {
        if (name.startsWith("{") && name.endsWith("}")) {
            String stripped = name.substring(1, name.length() - 1);

            // Handle special values
            if ("null".equals(stripped)) {
                return null;
            } else if ("true".equals(stripped)) {
                return true;
            } else if ("false".equals(stripped)) {
                return false;
            } else if (isNumeric(stripped)) {
                return Double.parseDouble(stripped);
            } else {
            	return extractFromWorld(world, stripped);
            }
        } else {
            return name;
        }
    }

    /**
     * Check if a string is numeric.
     *
     * @param str the string to check
     * @return true if the string represents a number
     */
    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private static JsonSchema findSchema(Map<String, JsonSchema> schemas, String name) {
        for (Map.Entry<String, JsonSchema> entry : schemas.entrySet()) {
            if (entry.getKey().endsWith(name + ".schema.json")) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Check if a table row matches the given data object.
     *
     * @param world the PropsWorld for resolving references
     * @param row the table row as a map of field names to expected values
     * @param data the actual data object to match against
     * @return true if the row matches the data
     */
    public static boolean doesRowMatch(PropsWorld world, Map<String, String> row, Object data) {
        for (Map.Entry<String, String> entry : row.entrySet()) {
            String field = entry.getKey();
            String expected = entry.getValue();

            if (field.endsWith("matches_type")) {
                // Schema validation mode
                Object valData = data;

                if (field.length() > "matches_type".length()) {
                    // Extract path before matches_type
                    String path = field.substring(0, field.length() - "matches_type".length() - 1);
                    valData = extractFromWorld(world, path);
                }

                // Validate against schema
                @SuppressWarnings("unchecked")
                Map<String, JsonSchema> schemas = (Map<String, JsonSchema>) world.get("schemas");
                if (schemas == null) {
                    world.log("No schemas loaded");
                    return false;
                }

                JsonSchema schema = findSchema(schemas,expected);
                
                if (schema == null) {
                    world.log("No schema found for " + expected);
                    return false;
                }

                try {
                    JsonNode dataNode = objectMapper.valueToTree(valData);
                    var errors = schema.validate(dataNode);
                    if (!errors.isEmpty()) {
                        world.log("Validation failed: " + errors);
                        return false;
                    }
                } catch (Exception e) {
                    world.log("Validation error: " + e.getMessage());
                    return false;
                }
            } else {
                // Field value comparison using JXPath
                try {
                    Object found = extractFromWorld(data, field);
                    Object resolved = handleResolve(expected, world);

                    if (!Objects.equals(asString(found), asString(resolved))) {
                        world.log(String.format(
                                "Match failed on %s: '%s' vs '%s'", field, found, resolved));
                        return false;
                    }
                } catch (JXPathNotFoundException e) {
                    world.log("Path not found: " + field);
                    return false;
                } catch (Exception e) {
                    world.log("Error: " + e.getMessage());
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Convert a value to string for comparison.
     */
    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Find the index of a matching row in the list.
     *
     * @param world the PropsWorld for resolving references
     * @param rows the list of expected rows
     * @param data the actual data object to find
     * @return the index of the matching row, or -1 if not found
     */
    public static int indexOf(PropsWorld world, List<Map<String, String>> rows, Object data) {
        for (int i = 0; i < rows.size(); i++) {
            if (doesRowMatch(world, rows.get(i), data)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Match an array of data against a Cucumber DataTable.
     *
     * @param world the PropsWorld for resolving references
     * @param actual the actual array data
     * @param dt the expected DataTable
     */
    public static void matchData(PropsWorld world, List<?> actual, DataTable dt) {
        List<Map<String, String>> tableData = dt.asMaps();
        int rowCount = tableData.size();

        world.log(String.format("result %s length %d",
                formatJson(actual), actual.size()));

        assertEquals(rowCount, actual.size(),
                "Array length mismatch");

        List<Object> resultCopy = new ArrayList<>(actual);
        List<Object> unmatched = new ArrayList<>();

        int row = 0;
        for (Object item : resultCopy) {
            Map<String, String> matchingRow = tableData.get(row);
            row++;
            if (!doesRowMatch(world, matchingRow, item)) {
                world.log("Couldn't match row: " + formatJson(item));
                unmatched.add(item);
            }
        }

        assertTrue(unmatched.isEmpty(),
                "Some rows could not be matched: " + formatJson(unmatched));
    }

    /**
     * Format an object as JSON for logging.
     */
    private static String formatJson(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }

    /**
     * Load JSON schemas from a directory.
     *
     * @param schemaDir the directory containing schema files
     * @return a map of schema names to JsonSchema objects
     */
    public static Map<String, JsonSchema> loadSchemas(String schemaDir) {
        Map<String, JsonSchema> schemas = new java.util.HashMap<>();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

        java.io.File dir = new java.io.File(schemaDir);
        if (dir.exists() && dir.isDirectory()) {
            java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files != null) {
                for (java.io.File file : files) {
                    try {
                        String content = java.nio.file.Files.readString(file.toPath());
                        JsonSchema schema = factory.getSchema(content);
                        String schemaName = file.getName().replace(".schema.json", "");
                        schemas.put(schemaName, schema);
                    } catch (Exception e) {
                        // Log and continue
                    }
                }
            }
        }

        return schemas;
    }
}

