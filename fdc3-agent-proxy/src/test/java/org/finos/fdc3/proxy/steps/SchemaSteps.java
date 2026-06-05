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

import java.io.IOException;

import org.finos.fdc3.proxy.schema.Fdc3SchemaMatchers;
import org.finos.fdc3.proxy.schema.LoadSchemas;

import io.cucumber.java.en.Given;
import io.github.robmoffat.world.PropsWorld;

/**
 * Cucumber glue for FDC3 schema loading and matchers.
 * Equivalent to {@code setupSchemaSteps()} and {@code registerFdc3SchemaMatchers()} in
 * {@code @finos/fdc3-schema/test}.
 */
public class SchemaSteps {

    static {
        Fdc3SchemaMatchers.registerFdc3SchemaMatchers();
    }

    private final PropsWorld world;

    public SchemaSteps(PropsWorld world) {
        this.world = world;
    }

    @Given("schemas loaded")
    public void schemasLoaded() throws IOException {
        LoadSchemas.loadSchemasIntoWorld(world);
    }
}
