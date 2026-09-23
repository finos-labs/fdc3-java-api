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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.finos.fdc3.api.errors.OpenError;
import org.finos.fdc3.proxy.util.Logger;
import org.finos.fdc3.proxy.util.ThrowIfUndefined;
import org.finos.fdc3.proxy.world.CustomWorld;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

/**
 * Cucumber step definitions for utility tests.
 */
public class UtilSteps {

    private final CustomWorld world;

    public UtilSteps(CustomWorld world) {
        this.world = world;
    }

    @Given("close will fail")
    public void closeWillFail() {
        if (world.hasMessaging()) {
            world.getMessaging().setCloseShouldFail(true);
        }
    }

    /**
     * A schema-valid response whose payload is empty, which is the shape
     * {@code throwIfUndefined} exists to catch.
     */
    private static Map<String, Object> dummyResponse() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("requestUuid", "123");
        meta.put("responseUuid", "456");
        meta.put("timestamp", Instant.now().toString());

        Map<String, Object> message = new HashMap<>();
        message.put("type", "broadcastResponse");
        message.put("meta", meta);
        message.put("payload", new HashMap<String, Object>());
        return message;
    }

    @When("I call throwIfUndefined it throws if a specified property is not defined")
    public void throwIfUndefinedThrows() {
        Map<String, String> someObject = new HashMap<>();
        someObject.put("someProperty", "value");

        Exception thrown = null;
        try {
            ThrowIfUndefined.throwIfUndefined(
                    someObject.get("nonExistent"),
                    "Deliberately undefined prop did not exist ;-)",
                    dummyResponse(),
                    OpenError.MalformedContext.toString());
        } catch (Exception e) {
            thrown = e;
        }

        assertNotNull(thrown, "throwIfUndefined should throw for an absent property");
        // The message has to be the bare FDC3 error name, since that is what applications match on.
        assertEquals(OpenError.MalformedContext.toString(), thrown.getMessage());
    }

    @When("I call throwIfUndefined it does NOT throw if a specified property IS defined")
    public void throwIfUndefinedDoesNotThrow() {
        Map<String, String> someObject = new HashMap<>();
        someObject.put("someProperty", "value");

        Exception thrown = null;
        try {
            ThrowIfUndefined.throwIfUndefined(
                    someObject.get("someProperty"),
                    "Deliberately undefined prop did not exist ;-)",
                    dummyResponse(),
                    OpenError.MalformedContext.toString());
        } catch (Exception e) {
            thrown = e;
        }

        assertNull(thrown, "throwIfUndefined should not throw for a property that is present");
    }

    @When("All log functions are used with a message")
    public void allLogFunctionsWithMessage() {
        Logger.debug("Debug msg");
        Logger.info("Log msg");
        Logger.warn("Warning msg");
        Logger.error("Error msg");
    }

    @When("All log functions are used with an error")
    public void allLogFunctionsWithError() {
        String testError = "Test error - This is expected on the console";
        Logger.debug("debug-level error: ", new Exception(testError));
        Logger.info("log-level error: ", new Exception(testError));
        Logger.warn("warn-level error: ", new Exception(testError));
        Logger.error("error-level error: ", new Exception(testError));
    }
}

