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

package com.finos.fdc3.testing.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finos.fdc3.testing.world.PropsWorld;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import io.cucumber.datatable.DataTable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Utility class for matching and resolving test data.
 * This is equivalent to the TypeScript matching.ts module.
 */
public final class MatchingUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private MatchingUtils() {
        // Utility class
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
                // Use JSONPath to resolve the value from props
                try {
                    Object propsAsJson = objectMapper.convertValue(world.getProps(), Object.class);
                    String jsonPath = "$." + stripped;
                    return JsonPath.read(propsAsJson, jsonPath);
                } catch (PathNotFoundException e) {
                    return null;
                }
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
                    try {
                        String dataJson = objectMapper.writeValueAsString(data);
                        valData = JsonPath.read(dataJson, "$." + path);
                    } catch (Exception e) {
                        world.log("Error extracting path: " + e.getMessage());
                        return false;
                    }
                }

                // Validate against schema
                @SuppressWarnings("unchecked")
                Map<String, JsonSchema> schemas = (Map<String, JsonSchema>) world.get("schemas");
                if (schemas == null) {
                    world.log("No schemas loaded");
                    return false;
                }

                JsonSchema schema = schemas.get(expected);
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
                // Field value comparison using JSONPath
                try {
                    String dataJson = objectMapper.writeValueAsString(data);
                    Object found = JsonPath.read(dataJson, "$." + field);
                    Object resolved = handleResolve(expected, world);

                    if (!Objects.equals(asString(found), asString(resolved))) {
                        world.log(String.format(
                                "Match failed on %s: '%s' vs '%s'", field, found, resolved));
                        return false;
                    }
                } catch (PathNotFoundException e) {
                    world.log("Path not found: " + field);
                    return false;
                } catch (JsonProcessingException e) {
                    world.log("JSON processing error: " + e.getMessage());
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

