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

package org.finos.fdc3.proxy;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.finos.fdc3.proxy.util.Logger;
import org.finos.fdc3.proxy.world.CustomWorld;

/**
 * Cucumber hooks for setting up and tearing down test state.
 */
public class CucumberHooks {

    private final CustomWorld world;

    public CucumberHooks(CustomWorld world) {
        this.world = world;
    }

    /**
     * Before each scenario, set the scenario on the world for logging.
     */
    @Before
    public void beforeScenario(Scenario scenario) {
        world.setScenario(scenario);
    }

    /**
     * Disconnects the messaging created by a scenario.
     * <p>
     * Scenarios register listeners and start timeout tasks through the messaging instance. Left
     * connected, those outlive the scenario and can still fire during later ones, so a failure
     * appears in whichever scenario happens to be running rather than the one that caused it.
     */
    @After
    public void afterScenario() {
        if (!world.hasMessaging()) {
            return;
        }
        try {
            world.getMessaging().disconnect().toCompletableFuture().join();
        } catch (RuntimeException e) {
            // Teardown only. Reporting this as a failure would mask the scenario's own result.
            Logger.warn("Failed to disconnect test messaging during teardown: {}", e.getMessage());
        }
    }
}

