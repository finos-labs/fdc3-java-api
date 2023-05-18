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

package com.finos.fdc3.api.types;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import com.finos.fdc3.api.context.Context;
import com.finos.fdc3.api.metadata.ContextMetadata;

/**
 * Describes a callback that handles a context event and may return a promise of a Context or Channel object to be returned to the
 * application that raised the intent. Used when attaching listeners for raised intents.
 */
@FunctionalInterface
public interface IntentHandler {

    /**
     * Handles an intent
     * 
     * @param context the context event
     * @param contextMetadata optional metadata
     * @return {@link Optional#empty()} if not result is required, or a {@link CompletionStage} that will be used to publish the
     *         intent result
     */
    Optional<CompletionStage<IntentResult>> handleIntent(Context context, ContextMetadata contextMetadata);

}
