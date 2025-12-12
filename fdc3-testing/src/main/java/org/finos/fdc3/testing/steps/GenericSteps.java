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

package org.finos.fdc3.testing.steps;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.finos.fdc3.testing.support.MatchingUtils;
import org.finos.fdc3.testing.world.PropsWorld;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.finos.fdc3.testing.support.MatchingUtils.doesRowMatch;
import static org.finos.fdc3.testing.support.MatchingUtils.handleResolve;
import static org.finos.fdc3.testing.support.MatchingUtils.matchData;
import static org.junit.jupiter.api.Assertions.*;

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

    @Then("{string} is an array of objects with the following contents")
    public void isAnArrayOfObjectsWithContents(String field, DataTable dt) {
        @SuppressWarnings("unchecked")
        List<Object> data = (List<Object>) handleResolve(field, world);
        matchData(world, data, dt);
    }

    @Then("{string} is an array of objects with length {string}")
    public void isAnArrayOfObjectsWithLength(String field, String lengthField) {
        @SuppressWarnings("unchecked")
        List<Object> data = (List<Object>) handleResolve(field, world);
        int expectedLength = ((Number) handleResolve(lengthField, world)).intValue();
        assertEquals(expectedLength, data.size());
    }

    @Then("{string} is an array of strings with the following values")
    public void isAnArrayOfStringsWithValues(String field, DataTable dt) {
        @SuppressWarnings("unchecked")
        List<String> data = (List<String>) handleResolve(field, world);
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
        @SuppressWarnings("unchecked")
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
        assertTrue(value instanceof Throwable);
        assertEquals(errorType, ((Throwable) value).getMessage());
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
        world.set(handlerName, (Runnable) () -> {
            int amount = (Integer) world.get(counterField);
            amount++;
            world.set(counterField, amount);
        });
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
        // Schema loading would be configured externally
        // The schemas map should be set up in the test configuration
        world.log("Schemas should be loaded by test configuration");
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
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
        }

        Method method = findMethod(target.getClass(), methodName, args.length);
        if (method == null) {
            throw new NoSuchMethodException("Method not found: " + methodName);
        }

        method.setAccessible(true);
        Object result = method.invoke(target, args);

        // If the result is a CompletionStage, resolve it
        return resolvePromise(result);
    }

    /**
     * Find a method by name and parameter count.
     */
    private Method findMethod(Class<?> clazz, String name, int paramCount) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == paramCount) {
                return method;
            }
        }
        return null;
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

