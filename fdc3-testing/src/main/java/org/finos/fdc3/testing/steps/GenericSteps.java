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

package org.finos.fdc3.testing.steps;

import static org.finos.fdc3.testing.support.MatchingUtils.doesRowMatch;
import static org.finos.fdc3.testing.support.MatchingUtils.handleResolve;
import static org.finos.fdc3.testing.support.MatchingUtils.matchData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.finos.fdc3.api.types.EventHandler;
import org.finos.fdc3.api.types.FDC3Event;
import org.finos.fdc3.testing.world.PropsWorld;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Generic Cucumber step definitions for FDC3 testing.
 * This is equivalent to the TypeScript generic.steps.ts module.
 */
public class GenericSteps {

    private final PropsWorld world;

    public GenericSteps(PropsWorld world) {
        this.world = world;
    }

    // ========== Promise Resolution Steps ==========

    @Then("the promise {string} should resolve")
    public void thePromiseShouldResolve(String field) {
        try {
            Object promise = handleResolve(field, world);
            Object result = resolvePromise(promise);
            world.set("result", result);
        } catch (Exception error) {
            world.set("result", error);
        }
    }

    @Then("the promise {string} should resolve within 10 seconds")
    public void thePromiseShouldResolveWithin10Seconds(String field) throws Exception {
        try {
            Object promise = handleResolve(field, world);
            Object result = resolvePromiseWithTimeout(promise, 10, TimeUnit.SECONDS);
            world.set("result", result);
        } catch (Exception error) {
            world.set("result", error);
        }
    }

    // ========== Method Invocation Steps ==========

    @When("I call {string} with {string}")
    public void iCallWith(String field, String fnName) {
        try {
            Object object = handleResolve(field, world);
            Object result = invokeMethod(object, fnName);
            world.set("result", result);
        } catch (Exception error) {
            world.set("result", error);
        }
    }

    @When("I call {string} with {string} with parameter {string}")
    public void iCallWithParameter(String field, String fnName, String param) {
        try {
            Object object = handleResolve(field, world);
            Object paramValue = handleResolve(param, world);
            Object result = invokeMethod(object, fnName, paramValue);
            world.set("result", result);
        } catch (Exception error) {
            world.set("result", error);
        }
    }

    @When("I call {string} with {string} with parameters {string} and {string}")
    public void iCallWithTwoParameters(String field, String fnName, String param1, String param2) {
        try {
            Object object = handleResolve(field, world);
            Object paramValue1 = handleResolve(param1, world);
            Object paramValue2 = handleResolve(param2, world);
            Object result = invokeMethod(object, fnName, paramValue1, paramValue2);
            world.set("result", result);
        } catch (Exception error) {
            world.set("result", error);
        }
    }

    @When("I call {string} with {string} with parameters {string} and {string} and {string}")
    public void iCallWithThreeParameters(String field, String fnName, String param1, String param2, String param3) {
        try {
            Object object = handleResolve(field, world);
            Object paramValue1 = handleResolve(param1, world);
            Object paramValue2 = handleResolve(param2, world);
            Object paramValue3 = handleResolve(param3, world);
            Object result = invokeMethod(object, fnName, paramValue1, paramValue2, paramValue3);
            world.set("result", result);
        } catch (Exception error) {
            world.set("result", error);
        }
    }

    // ========== Reference/Alias Steps ==========

    @When("I refer to {string} as {string}")
    public void iReferToAs(String from, String to) {
        world.set(to, handleResolve(from, world));
    }

    // ========== Array Matching Steps ==========

    /**
     * Convert an object to a List. Handles both arrays and Lists.
     */
    private List<Object> toList(Object obj) {
        if (obj == null) {
            return Collections.emptyList();
        }
        if (obj instanceof List) {
            return (List<Object>) obj;
        }
        if (obj.getClass().isArray()) {
            // Convert array to list
            int length = Array.getLength(obj);
            List<Object> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                list.add(Array.get(obj, i));
            }
            return list;
        }
        throw new IllegalArgumentException("Expected array or List, but got: " + obj.getClass().getName());
    }

    @Then("{string} is an array of objects with the following contents")
    public void isAnArrayOfObjectsWithContents(String field, DataTable dt) {
        Object resolved = handleResolve(field, world);
        List<Object> data = toList(resolved);
        matchData(world, data, dt);
    }

    @Then("{string} is an array of objects with length {string}")
    public void isAnArrayOfObjectsWithLength(String field, String lengthField) {
        Object resolved = handleResolve(field, world);
        List<Object> data = toList(resolved);
        String amt = (String) handleResolve(lengthField, world);
        assertEquals(Integer.parseInt(amt), data.size());
    }

    @Then("{string} is an array of strings with the following values")
    public void isAnArrayOfStringsWithValues(String field, DataTable dt) {
        Object resolved = handleResolve(field, world);
        List<Object> data = toList(resolved);
        List<Map<String, Object>> values = data.stream()
                .map(s -> Map.<String, Object>of("value", s))
                .collect(Collectors.toList());
        matchData(world, values, dt);
    }

    // ========== Object Matching Steps ==========

    @Then("{string} is an object with the following contents")
    public void isAnObjectWithContents(String field, DataTable params) {
        List<Map<String, String>> table = params.asMaps();
        Object data = handleResolve(field, world);
        assertTrue(doesRowMatch(world, table.get(0), data));
    }

    // ========== Null/Boolean/Undefined Checks ==========

    @Then("{string} is null")
    public void isNull(String field) {
        assertNull(handleResolve(field, world));
    }

    @Then("{string} is not null")
    public void isNotNull(String field) {
        assertNotNull(handleResolve(field, world));
    }

    @Then("{string} is true")
    public void isTrue(String field) {
        Object value = handleResolve(field, world);
        assertTrue(isTruthy(value));
    }

    @Then("{string} is false")
    public void isFalse(String field) {
        Object value = handleResolve(field, world);
        assertFalse(isTruthy(value));
    }

    @Then("{string} is undefined")
    public void isUndefined(String field) {
        // In Java, we treat undefined as null or non-existent in the props map
        Object value = handleResolve(field, world);
        assertNull(value);
    }

    @Then("{string} is empty")
    public void isEmpty(String field) {
        List<?> data = (List<?>) handleResolve(field, world);
        assertTrue(data.isEmpty());
    }

    // ========== Equality Check ==========

    @Then("{string} is {string}")
    public void fieldIsValue(String field, String expected) {
        Object fieldValue = handleResolve(field, world);
        Object expectedValue = handleResolve(expected, world);
        assertEquals(String.valueOf(expectedValue), String.valueOf(fieldValue));
    }

    // ========== Error Checks ==========

    @Then("{string} is an error with message {string}")
    public void isAnErrorWithMessage(String field, String errorType) {
        Object value = handleResolve(field, world);
        assertTrue(value instanceof Throwable, "Expected a Throwable but got: " + value);
        
        // Extract the root cause message - exceptions may be wrapped multiple times
        Throwable t = (Throwable) value;
        String message = getRootCauseMessage(t);
        assertEquals(errorType, message);
    }
    
    /**
     * Gets the message from the root cause of a potentially wrapped exception chain.
     */
    private String getRootCauseMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage();
    }

    @Then("{string} is an error")
    public void isAnError(String field) {
        Object value = handleResolve(field, world);
        assertTrue(value instanceof Throwable);
    }

    // ========== Invocation Counter ==========

    @Given("{string} is a invocation counter into {string}")
    public void isAnInvocationCounter(String handlerName, String counterField) {
        world.set(counterField, 0);
        EventHandler eh = new EventHandler() {
			
			@Override
			public void handleEvent(FDC3Event event) {
				int amount = (Integer) world.get(counterField);
		        amount++;
		        world.set(counterField, amount);				
			}
		};
        world.set(handlerName, eh);
    }

    // ========== Function Creation ==========

    @Given("{string} is a function which returns a promise of {string}")
    public void isAFunctionReturningPromise(String fnName, String valueField) {
        Object value = handleResolve(valueField, world);
        world.set(fnName, (Supplier<CompletableFuture<Object>>) () -> 
            CompletableFuture.completedFuture(value));
    }

    // ========== Wait Step ==========

    @Given("we wait for a period of {string} ms")
    public void weWaitForPeriod(String ms) throws InterruptedException {
        Thread.sleep(Long.parseLong(ms));
    }

    // ========== Schema Loading ==========

    @Given("schemas loaded")
    public void schemasLoaded() {
        Map<String, JsonSchema> schemas = new HashMap<>();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

        // Find the fdc3-schema module's npm-work directory
        // This works when running from the project root or any submodule
        Path schemaDir = findSchemaDirectory();
        
        if (schemaDir == null || !Files.exists(schemaDir)) {
            world.log("Schema directory not found. Run 'mvn compile' on fdc3-schema first.");
            return;
        }

        // Load all API schemas
        Path apiDir = schemaDir.resolve("api");
        if (Files.exists(apiDir)) {
            try (Stream<Path> files = Files.list(apiDir)) {
                files.filter(p -> p.toString().endsWith(".json"))
                     .forEach(file -> {
                         try {
                             String schemaContent = Files.readString(file);
                             JsonSchema schema = factory.getSchema(schemaContent);
                             // Use the schema $id or filename as the key
                             String schemaId = extractSchemaId(schemaContent, file.getFileName().toString());
                             schemas.put(schemaId, schema);
                         } catch (IOException e) {
                             world.log("Error loading schema " + file + ": " + e.getMessage());
                         }
                     });
            } catch (IOException e) {
                world.log("Error reading schema directory: " + e.getMessage());
            }
        }

        // Load context schema
        Path contextSchemaPath = findContextSchemaDirectory();
        if (contextSchemaPath != null) {
            Path contextSchema = contextSchemaPath.resolve("context").resolve("context.schema.json");
            if (Files.exists(contextSchema)) {
                try {
                    String schemaContent = Files.readString(contextSchema);
                    JsonSchema schema = factory.getSchema(schemaContent);
                    schemas.put("context", schema);
                    world.log("Loaded context schema");
                } catch (IOException e) {
                    world.log("Error loading context schema: " + e.getMessage());
                }
            }
        }

        world.set("schemas", schemas);
        world.log("Loaded " + schemas.size() + " schemas");
    }

    /**
     * Find the schema directory by checking various possible locations.
     */
    private Path findSchemaDirectory() {
        // Possible locations relative to current working directory
        String[] possiblePaths = {
            "fdc3-schema/target/npm-work/node_modules/@finos/fdc3-schema/dist/schemas",
            "../fdc3-schema/target/npm-work/node_modules/@finos/fdc3-schema/dist/schemas",
            "target/npm-work/node_modules/@finos/fdc3-schema/dist/schemas"
        };
        
        for (String pathStr : possiblePaths) {
            Path path = Paths.get(pathStr);
            if (Files.exists(path)) {
                return path;
            }
        }
        return null;
    }

    /**
     * Find the context schema directory.
     */
    private Path findContextSchemaDirectory() {
        String[] possiblePaths = {
            "fdc3-schema/target/npm-work/node_modules/@finos/fdc3-context/dist/schemas",
            "../fdc3-schema/target/npm-work/node_modules/@finos/fdc3-context/dist/schemas",
            "fdc3-context/target/npm-work/node_modules/@finos/fdc3-context/dist/schemas",
            "../fdc3-context/target/npm-work/node_modules/@finos/fdc3-context/dist/schemas"
        };
        
        for (String pathStr : possiblePaths) {
            Path path = Paths.get(pathStr);
            if (Files.exists(path)) {
                return path;
            }
        }
        return null;
    }

    /**
     * Extract the schema ID from the schema content or use the filename.
     */
    private String extractSchemaId(String schemaContent, String filename) {
        // Try to extract $id from the schema
        // Simple regex approach - could use Jackson for more robust parsing
        int idIndex = schemaContent.indexOf("\"$id\"");
        if (idIndex >= 0) {
            int colonIndex = schemaContent.indexOf(":", idIndex);
            int quoteStart = schemaContent.indexOf("\"", colonIndex + 1);
            int quoteEnd = schemaContent.indexOf("\"", quoteStart + 1);
            if (quoteStart >= 0 && quoteEnd > quoteStart) {
                return schemaContent.substring(quoteStart + 1, quoteEnd);
            }
        }
        // Fall back to filename without extension
        return filename.replace(".schema.json", "").replace(".json", "");
    }

    // ========== Helper Methods ==========

    /**
     * Resolve a promise/CompletionStage to its value.
     */
    private Object resolvePromise(Object promise) throws Exception {
        if (promise instanceof CompletionStage) {
            return ((CompletionStage<?>) promise).toCompletableFuture().get();
        } else if (promise instanceof CompletableFuture) {
            return ((CompletableFuture<?>) promise).get();
        }
        return promise;
    }

    /**
     * Resolve a promise with a timeout.
     */
    private Object resolvePromiseWithTimeout(Object promise, long timeout, TimeUnit unit) throws Exception {
        if (promise instanceof CompletionStage) {
            return ((CompletionStage<?>) promise).toCompletableFuture().get(timeout, unit);
        } else if (promise instanceof CompletableFuture) {
            return ((CompletableFuture<?>) promise).get(timeout, unit);
        }
        return promise;
    }

    /**
     * Invoke a method on an object by name.
     */
    private Object invokeMethod(Object target, String methodName, Object... args) throws Exception {
        Method method = findMethod(target.getClass(), methodName, args);
        if (method == null) {
            throw new NoSuchMethodException("Method not found: " + methodName);
        }

        method.setAccessible(true);
        Object result = method.invoke(target, args);

        // If the result is a CompletionStage, resolve it
        return resolvePromise(result);
    }

  
    public static Method findMethod(
            Class<?> targetClass,
            String name,
            Object... args
    ) {
        Method bestMatch = null;

        for (Method method : targetClass.getMethods()) {
            if (!method.getName().equals(name)) continue;

            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != args.length) continue;

            if (isCompatible(paramTypes, args)) {
                if (bestMatch == null ||
                        isMoreSpecific(paramTypes, bestMatch.getParameterTypes())) {
                    bestMatch = method;
                }
            }
        }

        return bestMatch;
    }

    private static boolean isCompatible(Class<?>[] paramTypes, Object[] args) {
        for (int i = 0; i < paramTypes.length; i++) {
            if (args[i] == null) {
                if (paramTypes[i].isPrimitive()) return false;
                continue;
            }

            Class<?> argType = args[i].getClass();
            if (!wrap(paramTypes[i]).isAssignableFrom(argType)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isMoreSpecific(Class<?>[] a, Class<?>[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i] && b[i].isAssignableFrom(a[i])) {
                return true;
            }
        }
        return false;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == boolean.class) return Boolean.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == char.class) return Character.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        return type;
    }

    /**
     * Check if a value is truthy (JavaScript-style).
     */
    private boolean isTruthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0;
        }
        if (value instanceof String) {
            return !((String) value).isEmpty();
        }
        return true;
    }
}

