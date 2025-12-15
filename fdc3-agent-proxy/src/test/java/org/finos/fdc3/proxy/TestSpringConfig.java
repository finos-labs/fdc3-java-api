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

package org.finos.fdc3.proxy;

import io.cucumber.spring.ScenarioScope;
import org.finos.fdc3.proxy.world.CustomWorld;
import org.finos.fdc3.testing.world.PropsWorld;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for Cucumber tests.
 * 
 * This configuration ensures that:
 * 1. CustomWorld is used as the shared world instance
 * 2. PropsWorld injections receive the CustomWorld instance
 */
@Configuration
@ComponentScan(basePackages = {
    "org.finos.fdc3.proxy.steps",
    "org.finos.fdc3.testing.steps"
})
public class TestSpringConfig {

    /**
     * Create CustomWorld as a scenario-scoped bean.
     */
    @Bean
    @ScenarioScope
    public CustomWorld customWorld() {
        return new CustomWorld();
    }

    /**
     * Provide CustomWorld as the implementation for PropsWorld.
     * This ensures GenericSteps (which depends on PropsWorld) gets
     * the same instance as AgentSteps (which depends on CustomWorld).
     */
    @Bean
    @Primary
    @ScenarioScope
    public PropsWorld propsWorld(CustomWorld customWorld) {
        return customWorld;
    }
}

