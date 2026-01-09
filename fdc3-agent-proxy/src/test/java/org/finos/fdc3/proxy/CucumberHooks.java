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

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
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
}

