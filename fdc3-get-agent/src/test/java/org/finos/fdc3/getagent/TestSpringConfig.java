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

package org.finos.fdc3.getagent;

import io.github.robmoffat.world.PropsWorld;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;

import io.cucumber.spring.ScenarioScope;

/**
 * Spring configuration for Cucumber tests.
 *
 * Scans GetAgent-specific step definitions and generic steps from standard-cucumber-steps.
 */
@Configuration
@ComponentScan(basePackages = {
    "org.finos.fdc3.getagent.steps",
    "io.github.robmoffat.steps"
})
public class TestSpringConfig {

    @Bean
    @ScenarioScope(proxyMode = ScopedProxyMode.NO)
    public PropsWorld propsWorld() {
        return new PropsWorld();
    }
}
