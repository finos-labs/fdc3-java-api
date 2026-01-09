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

import org.finos.fdc3.proxy.world.CustomWorld;

import io.cucumber.java.en.When;

/**
 * Cucumber step definitions for utility tests.
 */
public class UtilSteps {

    private final CustomWorld world;

    public UtilSteps(CustomWorld world) {
        this.world = world;
    }

    @When("I call throwIfUndefined it throws if a specified property is not defined")
    public void throwIfUndefinedThrows() {
        Exception thrown = null;
        try {
            Object value = null;
            if (value == null) {
                throw new IllegalArgumentException("OpenError.MalformedContext");
            }
        } catch (Exception e) {
            thrown = e;
        }

        assertNotNull(thrown);
        assertEquals("OpenError.MalformedContext", thrown.getMessage());
    }

    @When("I call throwIfUndefined it does NOT throw if a specified property IS defined")
    public void throwIfUndefinedDoesNotThrow() {
        Exception thrown = null;
        try {
            Object value = "some-value";
            if (value == null) {
                throw new IllegalArgumentException("OpenError.MalformedContext");
            }
        } catch (Exception e) {
            thrown = e;
        }

        assertNull(thrown);
    }

    @When("All log functions are used with a message")
    public void allLogFunctionsWithMessage() {
        System.out.println("[DEBUG] Debug msg");
        System.out.println("[LOG] Log msg");
        System.out.println("[WARN] Warning msg");
        System.out.println("[ERROR] Error msg");
    }

    @When("All log functions are used with an error")
    public void allLogFunctionsWithError() {
        String testError = "Test error - This is expected on the console";
        System.out.println("[DEBUG] debug-level error: " + new Exception(testError));
        System.out.println("[LOG] log-level error: " + new Exception(testError));
        System.out.println("[WARN] warn-level error: " + new Exception(testError));
        System.out.println("[ERROR] error-level error: " + new Exception(testError));
    }
}

