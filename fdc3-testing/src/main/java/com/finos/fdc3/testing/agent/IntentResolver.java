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

package com.finos.fdc3.testing.agent;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import com.finos.fdc3.api.context.Context;
import com.finos.fdc3.api.metadata.AppIntent;
import com.finos.fdc3.api.types.IntentResult;

/**
 * Interface for resolving intents to specific applications.
 * <p>
 * Implementations of this interface handle the user interaction
 * for selecting an intent and target application when multiple
 * options are available.
 */
public interface IntentResolver {

    /**
     * Connect the resolver (e.g., initialize UI components).
     *
     * @return a CompletionStage that completes when connected
     */
    CompletionStage<Void> connect();

    /**
     * Disconnect the resolver (e.g., cleanup UI components).
     *
     * @return a CompletionStage that completes when disconnected
     */
    CompletionStage<Void> disconnect();

    /**
     * Called when an intent has been chosen and resolved.
     *
     * @param intentResult the result of the intent resolution
     * @return a CompletionStage containing the intent result
     */
    CompletionStage<IntentResult> intentChosen(IntentResult intentResult);

    /**
     * Choose an intent from a list of available intents.
     *
     * @param appIntents the list of available app intents
     * @param context    the context for the intent
     * @return a CompletionStage containing the chosen resolution, or empty if cancelled
     */
    CompletionStage<Optional<SimpleIntentResolver.IntentResolutionChoice>> chooseIntent(
            List<AppIntent> appIntents, Context context);
}

