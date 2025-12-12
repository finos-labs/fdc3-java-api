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

package org.finos.fdc3.proxy.intents;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.metadata.AppIntent;

/**
 * Interface for intent resolution UI components.
 */
public interface IntentResolver {

    /**
     * Display a UI to let the user choose an intent and app.
     *
     * @param appIntents the available intents and apps
     * @param context    the context being passed
     * @return a CompletionStage containing the user's choice, or null if cancelled
     */
    CompletionStage<IntentResolutionChoice> chooseIntent(List<AppIntent> appIntents, Context context);
}

