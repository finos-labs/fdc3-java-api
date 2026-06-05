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

package org.finos.fdc3.proxy.steps;

import static io.github.robmoffat.support.MatchingUtils.doesRowMatch;
import static io.github.robmoffat.support.MatchingUtils.handleResolve;
import static io.github.robmoffat.support.MatchingUtils.matchData;
import static io.github.robmoffat.support.MatchingUtils.matchDataAtLeast;
import static io.github.robmoffat.support.MatchingUtils.matchDataDoesntContain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.finos.fdc3.api.types.EventHandler;

import io.github.robmoffat.world.PropsWorld;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Generic Cucumber step definitions (local copy of standard-cucumber-steps with FDC3-specific adjustments).
 */
public class Fdc3GenericSteps {

    private final PropsWorld world;
    private final Map<String, CompletableFuture<Object>> jobs = new HashMap<>();

    public Fdc3GenericSteps(PropsWorld world) {
        this.world = world;
    }

    // Functional interfaces for multi-arg functions
    @FunctionalInterface
    public interface ThreeArgFunction {
        CompletableFuture<Object> apply(Object a, Object b, Object c);
    }
    
    @FunctionalInterface
    public interface FourArgFunction {
        CompletableFuture<Object> apply(Object a, Object b, Object c, Object d);
    }

    // ========== Method Invocation (Object.method) Steps ==========

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

    @When("I call {string} with {string} using argument {string}")
    public void iCallWithArgument(String field, String fnName, String param) {
        try {
            Object object = handleResolve(field, world);
            Object paramValue = handleResolve(param, world);
            Object result = invokeMethod(object, fnName, paramValue);
            world.set("result", result);
        } catch (Exception error) {
            world.set("result", error);
        }
    }

    @When("I call {string} with {string} using arguments {string} and {string}")
    public void iCallWithTwoArguments(String field, String fnName, String param1, String param2) {
        try {
            Object object = handleResolve(field, world);
            Object result = invokeMethod(object, fnName, handleResolve(param1, world), handleResolve(param2, world));
            world.set("result", result);
        } catch (Exception error) {
            world.set("result", error);
        }
    }

    @When("I call {string} with {string} using arguments {string}, {string}, and {string}")
    public void iCallWithThreeArguments(String field, String fnName, String param1, String param2, String param3) {
        try {
            Object object = handleResolve(field, world);
            Object result = invokeMethod(object, fnName,
                    handleResolve(param1, world), handleResolve(param2, world), handleResolve(param3, world));
            world.set("result", result);
        } catch (Exception error) {
            world.set("result", error);
        }
    }

    @When("I call {string} with {string} using arguments {string}, {string}, {string}, and {string}")
    public void iCallWithFourArguments(String field, String fnName, String param1, String param2, String param3, String param4) {
        try {
            Object object = handleResolve(field, world);
            Object result = invokeMethod(object, fnName,
                    handleResolve(param1, world), handleResolve(param2, world), 
                    handleResolve(param3, world), handleResolve(param4, world));
            world.set("result", result);
        } catch (Exception error) {
            world.set("result", error);
        }
    }

    @When("I call {string} with {string} as {string}")
    public void startMethodJob(String field, String fnName, String jobName) {
        startMethodJobWithArgs(field, fnName, jobName);
    }

    @When("I call {string} with {string} using argument {string} as {string}")
    public void startMethodJobWithArgument(String field, String fnName, String param, String jobName) {
        startMethodJobWithArgs(field, fnName, jobName, handleResolve(param, world));
    }

    @When("I call {string} with {string} using arguments {string} and {string} as {string}")
    public void startMethodJobWithTwoArguments(String field, String fnName, String param1, String param2, String jobName) {
        startMethodJobWithArgs(field, fnName, jobName, handleResolve(param1, world), handleResolve(param2, world));
    }

    @When("I call {string} with {string} using arguments {string}, {string}, and {string} as {string}")
    public void startMethodJobWithThreeArguments(String field, String fnName, String param1, String param2, String param3, String jobName) {
        startMethodJobWithArgs(field, fnName, jobName,
                handleResolve(param1, world), handleResolve(param2, world), handleResolve(param3, world));
    }

    @When("I call {string} with {string} using arguments {string}, {string}, {string}, and {string} as {string}")
    public void startMethodJobWithFourArguments(String field, String fnName, String param1, String param2, String param3, String param4, String jobName) {
        startMethodJobWithArgs(field, fnName, jobName,
                handleResolve(param1, world), handleResolve(param2, world),
                handleResolve(param3, world), handleResolve(param4, world));
    }

    private void startMethodJobWithArgs(String field, String fnName, String jobName, Object... args) {
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
            try {
                Object object = handleResolve(field, world);
                return invokeMethod(object, fnName, args);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        jobs.put(jobName, future);
    }

    // ========== Direct Function Call Steps ==========

    @When("I call {string}")
    public void iCallFunction(String fnName) {
        try {
            Object fn = handleResolve(fnName, world);
            Object result = callFunctional(fn);
            world.set("result", result);
        } catch (Exception error) {
            world.set("result", error);
        }
    }

    @When("I call {string} using argument {string}")
    public void iCallFunctionWithArgument(String fnName, String param) {
        try {
            Object fn = handleResolve(fnName, world);
            Object paramVal = handleResolve(param, world);
            Object result = callFunctionalWithArgs(fn, paramVal);
            world.set("result", result);
        } catch (Exception error) {
            world.set("result", error);
        }
    }

    @When("I call {string} using arguments {string} and {string}")
    public void iCallFunctionWithTwoArguments(String fnName, String param1, String param2) {
        try {
            Object fn = handleResolve(fnName, world);
            Object result = callFunctionalWithArgs(fn, handleResolve(param1, world), handleResolve(param2, world));
            world.set("result", result);
        } catch (Exception error) {
            world.set("result", error);
        }
    }

    @When("I call {string} using arguments {string}, {string}, and {string}")
    public void iCallFunctionWithThreeArguments(String fnName, String param1, String param2, String param3) {
        try {
            Object fn = handleResolve(fnName, world);
            Object result = callFunctionalWithArgs(fn,
                    handleResolve(param1, world), handleResolve(param2, world), handleResolve(param3, world));
            world.set("result", result);
        } catch (Exception error) {
            world.set("result", error);
        }
    }

    @When("I call {string} using arguments {string}, {string}, {string}, and {string}")
    public void iCallFunctionWithFourArguments(String fnName, String param1, String param2, String param3, String param4) {
        try {
            Object fn = handleResolve(fnName, world);
            Object result = callFunctionalWithArgs(fn,
                    handleResolve(param1, world), handleResolve(param2, world), 
                    handleResolve(param3, world), handleResolve(param4, world));
            world.set("result", result);
        } catch (Exception error) {
            world.set("result", error);
        }
    }

    // ========== Variable Reference ==========

    @When("I refer to {string} as {string}")
    public void iReferToAs(String from, String to) {
        world.set(to, handleResolve(from, world));
    }

    // ========== Array Matching Steps ==========

    @Then("{string} is an array of objects with the following contents")
    public void isAnArrayOfObjectsWithContents(String field, DataTable dt) {
        matchData(world, toList(handleResolve(field, world)), dt);
    }

    @Then("{string} is an array of objects with at least the following contents")
    public void isAnArrayOfObjectsWithAtLeastContents(String field, DataTable dt) {
        matchDataAtLeast(world, toList(handleResolve(field, world)), dt);
    }

    @Then("{string} is an array of objects which doesn't contain any of")
    public void isAnArrayOfObjectsWhichDoesntContainAnyOf(String field, DataTable dt) {
        matchDataDoesntContain(world, toList(handleResolve(field, world)), dt);
    }

    @Then("{string} is an array of objects with length {string}")
    public void isAnArrayOfObjectsWithLength(String field, String lengthField) {
        List<Object> data = toList(handleResolve(field, world));
        Object resolved = handleResolve(lengthField, world);
        assertEquals(Integer.parseInt(String.valueOf(resolved)), data.size());
    }

    @Then("{string} is an array of strings with the following values")
    public void isAnArrayOfStringsWithValues(String field, DataTable dt) {
        List<Object> data = toList(handleResolve(field, world));
        List<Map<String, Object>> values = data.stream()
                .map(s -> Map.<String, Object>of("value", s))
                .collect(Collectors.toList());
        matchData(world, values, dt);
    }

    @Then("{string} is an object with the following contents")
    public void isAnObjectWithContents(String field, DataTable params) {
        List<Map<String, String>> table = params.asMaps();
        Object data = handleResolve(field, world);
        assertTrue(doesRowMatch(world, table.get(0), data));
    }

    // ========== Value Assertions ==========

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
        assertTrue(isTruthy(handleResolve(field, world)));
    }

    @Then("{string} is false")
    public void isFalse(String field) {
        assertFalse(isTruthy(handleResolve(field, world)));
    }

    @Then("{string} is undefined")
    public void isUndefined(String field) {
        assertNull(handleResolve(field, world));
    }

    @Then("{string} is empty")
    public void isEmpty(String field) {
        Object data = handleResolve(field, world);
        if (data instanceof List) {
            assertTrue(((List<?>) data).isEmpty());
        } else if (data instanceof String) {
            assertTrue(((String) data).isEmpty());
        } else if (data == null) {
            // null is considered empty
        } else {
            throw new AssertionError("Expected empty collection or string, got: " + data.getClass().getName());
        }
    }

    // ========== Error Assertions ==========

    @Then("{string} is an error with message {string}")
    public void isAnErrorWithMessage(String field, String errorType) {
        Object value = handleResolve(field, world);
        assertTrue(value instanceof Throwable, "Expected a Throwable but got: " + value);
        Throwable t = (Throwable) value;
        String message = getRootCauseMessage(t);
        assertEquals(errorType, message);
    }

    @Then("{string} is an error")
    public void isAnError(String field) {
        assertTrue(handleResolve(field, world) instanceof Throwable);
    }

    @Then("{string} is not an error")
    public void isNotAnError(String field) {
        assertFalse(handleResolve(field, world) instanceof Throwable);
    }

    @Then("{string} contains {string}")
    public void contains(String field, String substring) {
        String actual = String.valueOf(handleResolve(field, world));
        assertTrue(actual.contains(substring), "Expected '" + actual + "' to contain '" + substring + "'");
    }

    @Then("{string} is a string containing one of")
    public void isAStringContainingOneOf(String field, DataTable dt) {
        String actual = String.valueOf(handleResolve(field, world));
        List<List<String>> rows = dt.cells();
        List<String> values = rows.stream().skip(1).map(r -> r.get(0)).collect(Collectors.toList());
        boolean found = values.stream().anyMatch(actual::contains);
        assertTrue(found, "Expected '" + actual + "' to contain one of: " + values);
    }

    @Then("{string} should be greater than {string}")
    public void shouldBeGreaterThan(String field, String threshold) {
        double actual = toDouble(handleResolve(field, world));
        double thresh = toDouble(handleResolve(threshold, world));
        assertTrue(actual > thresh, "Expected " + actual + " > " + thresh);
    }

    @Then("{string} should be less than {string}")
    public void shouldBeLessThan(String field, String threshold) {
        double actual = toDouble(handleResolve(field, world));
        double thresh = toDouble(handleResolve(threshold, world));
        assertTrue(actual < thresh, "Expected " + actual + " < " + thresh);
    }

    // ========== Test Setup ==========

    @Given("{string} is a invocation counter into {string}")
    public void isAnInvocationCounter(String handlerName, String counterField) {
        world.set(counterField, 0);
        world.set(handlerName, (EventHandler) event -> {
            int amount = (Integer) world.get(counterField);
            world.set(counterField, amount + 1);
        });
    }

    @Given("{string} is an async function returning {string}")
    public void isAnAsyncFunctionReturning(String fnName, String valueField) {
        Object value = handleResolve(valueField, world);
        world.set(fnName, (Supplier<CompletableFuture<Object>>) () ->
                CompletableFuture.completedFuture(value));
    }

    @Given("{string} is an async function returning {string} after {string} ms")
    public void isAnAsyncFunctionReturningAfterDelay(String fnName, String valueField, String delayMs) {
        Object value = handleResolve(valueField, world);
        long delay = Long.parseLong(delayMs);
        world.set(fnName, (Supplier<CompletableFuture<Object>>) () ->
                CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(delay);
                        return value;
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }));
    }

    // Setter step: I set "field" to "value"
    @Given("I set {string} to {string}")
    public void iSetFieldTo(String field, String value) {
        world.set(field, handleResolve(value, world));
    }

    // Assertion step: "{field}" is "value"
    @Then("{string} is {string}")
    public void fieldIs(String field, String value) {
        Object actual = handleResolve(field, world);
        Object expected = handleResolve(value, world);
        assertEquals(String.valueOf(expected), String.valueOf(actual));
    }

    @Given("we wait for a period of {string} ms")
    public void weWaitForPeriod(String ms) throws InterruptedException {
        Thread.sleep(Long.parseLong(ms));
    }

    // ========== Async Job Steps ==========

    @When("I start {string} as {string}")
    public void startJob(String fnName, String jobName) {
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
            try {
                Object fn = handleResolve(fnName, world);
                return callFunctional(fn);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        jobs.put(jobName, future);
    }

    @When("I start {string} using argument {string} as {string}")
    public void startJobWithArgument(String fnName, String param, String jobName) {
        Object paramVal = handleResolve(param, world);
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
            try {
                Object fn = handleResolve(fnName, world);
                return callFunctionalWithArgs(fn, paramVal);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        jobs.put(jobName, future);
    }

    @When("I start {string} using arguments {string} and {string} as {string}")
    public void startJobWithTwoArguments(String fnName, String param1, String param2, String jobName) {
        Object p1 = handleResolve(param1, world);
        Object p2 = handleResolve(param2, world);
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
            try {
                Object fn = handleResolve(fnName, world);
                return callFunctionalWithArgs(fn, p1, p2);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        jobs.put(jobName, future);
    }

    @When("I start {string} using arguments {string}, {string}, and {string} as {string}")
    public void startJobWithThreeArguments(String fnName, String param1, String param2, String param3, String jobName) {
        Object p1 = handleResolve(param1, world);
        Object p2 = handleResolve(param2, world);
        Object p3 = handleResolve(param3, world);
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
            try {
                Object fn = handleResolve(fnName, world);
                return callFunctionalWithArgs(fn, p1, p2, p3);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        jobs.put(jobName, future);
    }

    @When("I start {string} using arguments {string}, {string}, {string}, and {string} as {string}")
    public void startJobWithFourArguments(String fnName, String param1, String param2, String param3, String param4, String jobName) {
        Object p1 = handleResolve(param1, world);
        Object p2 = handleResolve(param2, world);
        Object p3 = handleResolve(param3, world);
        Object p4 = handleResolve(param4, world);
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
            try {
                Object fn = handleResolve(fnName, world);
                return callFunctionalWithArgs(fn, p1, p2, p3, p4);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        jobs.put(jobName, future);
    }

    @Then("I wait for job {string}")
    public void waitForJob(String jobName) {
        try {
            Object result = jobs.get(jobName).get(30, TimeUnit.SECONDS);
            world.set("result", result);
            world.set(jobName, result);
        } catch (Exception e) {
            world.set("result", e);
            world.set(jobName, e);
        }
    }

    @Then("I wait for job {string} within {string} ms")
    public void waitForJobWithTimeout(String jobName, String timeoutMs) {
        try {
            long ms = Long.parseLong(timeoutMs);
            Object result = jobs.get(jobName).get(ms, TimeUnit.MILLISECONDS);
            world.set("result", result);
            world.set(jobName, result);
        } catch (Exception e) {
            world.set("result", e);
            world.set(jobName, e);
        }
    }

    @When("I wait for {string}")
    public void iWaitFor(String fnName) {
        try {
            Object fn = handleResolve(fnName, world);
            Object result = callFunctional(fn);
            world.set("result", result);
        } catch (Exception e) {
            world.set("result", e);
        }
    }

    @When("I wait for {string} within {string} ms")
    public void iWaitForWithTimeout(String fnName, String timeoutMs) {
        try {
            Object fn = handleResolve(fnName, world);
            long ms = Long.parseLong(timeoutMs);
            CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return callFunctional(fn);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Object result = future.get(ms, TimeUnit.MILLISECONDS);
            world.set("result", result);
        } catch (Exception e) {
            world.set("result", e);
        }
    }

    @When("I wait for {string} using argument {string}")
    public void iWaitForWithArgument(String fnName, String param) {
        try {
            Object fn = handleResolve(fnName, world);
            Object result = callFunctionalWithArgs(fn, handleResolve(param, world));
            world.set("result", result);
        } catch (Exception e) {
            world.set("result", e);
        }
    }

    @When("I wait for {string} using arguments {string} and {string}")
    public void iWaitForWithTwoArguments(String fnName, String param1, String param2) {
        try {
            Object fn = handleResolve(fnName, world);
            Object result = callFunctionalWithArgs(fn, handleResolve(param1, world), handleResolve(param2, world));
            world.set("result", result);
        } catch (Exception e) {
            world.set("result", e);
        }
    }

    @When("I wait for {string} using arguments {string}, {string}, and {string}")
    public void iWaitForWithThreeArguments(String fnName, String param1, String param2, String param3) {
        try {
            Object fn = handleResolve(fnName, world);
            Object result = callFunctionalWithArgs(fn, 
                    handleResolve(param1, world), handleResolve(param2, world), handleResolve(param3, world));
            world.set("result", result);
        } catch (Exception e) {
            world.set("result", e);
        }
    }

    @When("I wait for {string} using arguments {string}, {string}, {string}, and {string}")
    public void iWaitForWithFourArguments(String fnName, String param1, String param2, String param3, String param4) {
        try {
            Object fn = handleResolve(fnName, world);
            Object result = callFunctionalWithArgs(fn, 
                    handleResolve(param1, world), handleResolve(param2, world), 
                    handleResolve(param3, world), handleResolve(param4, world));
            world.set("result", result);
        } catch (Exception e) {
            world.set("result", e);
        }
    }

    // ========== Helper Methods ==========

    private Object callFunctional(Object fn) throws Exception {
        if (fn instanceof Runnable) {
            ((Runnable) fn).run();
            return null;
        }
        if (fn instanceof java.util.concurrent.Callable) {
            Object result = ((java.util.concurrent.Callable<?>) fn).call();
            return resolvePromise(result);
        }
        if (fn instanceof Supplier) {
            Object result = ((Supplier<?>) fn).get();
            return resolvePromise(result);
        }
        throw new IllegalArgumentException("Not a callable: " + (fn == null ? "null" : fn.getClass().getName()));
    }

    private Object callFunctionalWithArgs(Object fn, Object... args) throws Exception {
        Method invokeMethod = findMethod(fn.getClass(), "apply", args);
        if (invokeMethod == null) {
            invokeMethod = findMethod(fn.getClass(), "accept", args);
        }
        if (invokeMethod == null) {
            invokeMethod = findMethod(fn.getClass(), "call", args);
        }
        if (invokeMethod != null) {
            invokeMethod.setAccessible(true);
            Object result = invokeMethod.invoke(fn, args);
            return resolvePromise(result);
        }
        throw new IllegalArgumentException("Cannot call " + fn.getClass().getName() + " with " + args.length + " args");
    }

    private Object resolvePromise(Object promise) throws Exception {
        if (promise instanceof Supplier) {
            promise = ((Supplier<?>) promise).get();
        }
        if (promise instanceof CompletionStage) {
            return ((CompletionStage<?>) promise).toCompletableFuture().get(30, TimeUnit.SECONDS);
        }
        return promise;
    }

    private Object invokeMethod(Object target, String methodName, Object... args) throws Exception {
        Method method = findMethod(target.getClass(), methodName, args);
        if (method == null) {
            throw new NoSuchMethodException("Method not found: " + methodName);
        }
        method.setAccessible(true);
        // Convert args to match parameter types
        Object[] convertedArgs = convertArgs(method.getParameterTypes(), args);
        Object result = method.invoke(target, convertedArgs);
        return resolvePromise(result);
    }

    private Object[] convertArgs(Class<?>[] paramTypes, Object[] args) {
        Object[] converted = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            converted[i] = convertArg(paramTypes[i], args[i]);
        }
        return converted;
    }

    private Object convertArg(Class<?> targetType, Object arg) {
        if (arg == null) return null;
        
        // If already compatible, return as-is
        if (wrap(targetType).isAssignableFrom(arg.getClass())) {
            return arg;
        }
        
        // Handle numeric conversions
        Number num = null;
        if (arg instanceof Number) {
            num = (Number) arg;
        } else if (arg instanceof String) {
            try {
                num = Double.parseDouble((String) arg);
            } catch (NumberFormatException e) {
                // Not a number, check for char
                if ((targetType == char.class || targetType == Character.class) && ((String) arg).length() == 1) {
                    return ((String) arg).charAt(0);
                }
                return arg;
            }
        }
        
        if (num != null) {
            if (targetType == int.class || targetType == Integer.class) return num.intValue();
            if (targetType == long.class || targetType == Long.class) return num.longValue();
            if (targetType == double.class || targetType == Double.class) return num.doubleValue();
            if (targetType == float.class || targetType == Float.class) return num.floatValue();
            if (targetType == short.class || targetType == Short.class) return num.shortValue();
            if (targetType == byte.class || targetType == Byte.class) return num.byteValue();
        }
        
        return arg;
    }

    public static Method findMethod(Class<?> targetClass, String name, Object... args) {
        Method bestMatch = null;
        for (Method method : targetClass.getMethods()) {
            if (!method.getName().equals(name)) continue;
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != args.length) continue;
            if (isCompatible(paramTypes, args)) {
                if (bestMatch == null || isMoreSpecific(paramTypes, bestMatch.getParameterTypes())) {
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
            Class<?> wrapped = wrap(paramTypes[i]);
            Class<?> argClass = args[i].getClass();
            // Direct assignment compatibility
            if (wrapped.isAssignableFrom(argClass)) continue;
            // Handle numeric conversions: any Number can be converted to any numeric primitive
            if (args[i] instanceof Number && isNumericType(paramTypes[i])) continue;
            // Handle String to numeric conversion
            if (args[i] instanceof String && isNumericType(paramTypes[i])) {
                try {
                    Double.parseDouble((String) args[i]);
                    continue;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            // Handle String to char conversion
            if (args[i] instanceof String && (paramTypes[i] == char.class || paramTypes[i] == Character.class)) {
                if (((String) args[i]).length() == 1) continue;
            }
            return false;
        }
        return true;
    }

    private static boolean isNumericType(Class<?> type) {
        return type == int.class || type == Integer.class ||
               type == long.class || type == Long.class ||
               type == double.class || type == Double.class ||
               type == float.class || type == Float.class ||
               type == short.class || type == Short.class ||
               type == byte.class || type == Byte.class;
    }

    private static boolean isMoreSpecific(Class<?>[] a, Class<?>[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i] && b[i].isAssignableFrom(a[i])) return true;
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

    private String getRootCauseMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage();
    }

    private boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0;
        if (value instanceof String) {
            String s = (String) value;
            if (s.isEmpty()) return false;
            try { return Double.parseDouble(s) != 0; } catch (NumberFormatException ignored) {}
            return !s.equalsIgnoreCase("false") && !s.equalsIgnoreCase("null");
        }
        return true;
    }

    private double toDouble(Object value) {
        return Double.parseDouble(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private List<Object> toList(Object obj) {
        if (obj == null) return Collections.emptyList();
        if (obj instanceof List) return (List<Object>) obj;
        if (obj.getClass().isArray()) {
            int length = Array.getLength(obj);
            List<Object> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) list.add(Array.get(obj, i));
            return list;
        }
        throw new IllegalArgumentException("Expected array or List, but got: " + obj.getClass().getName());
    }
}
