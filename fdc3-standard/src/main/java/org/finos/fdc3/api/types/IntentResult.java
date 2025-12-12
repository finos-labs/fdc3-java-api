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
package org.finos.fdc3.api.types;

import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.schema.Context;
import org.finos.fdc3.api.metadata.IntentResolution;

/**
 * Describes results that an {@link IntentHandler} may optionally return that should be communicated back to the app that raised the
 * intent, via the {@link IntentResolution}. Represented as a union type in TypeScript, however in Java it is a marker interface
 * implemented by {@link Context} and {@link Channel}
 */
public interface IntentResult {
    /**
     * Get the underlying value of this result.
     * This may be a Context, Channel, or null.
     */
    default Object getValue() {
        return this;
    }
}
