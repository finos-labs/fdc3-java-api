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

package com.finos.fdc3.proxy.intents;

import java.util.List;
import java.util.concurrent.CompletionStage;

import com.finos.fdc3.api.context.Context;
import com.finos.fdc3.api.metadata.AppIntent;
import com.finos.fdc3.api.metadata.IntentResolution;
import com.finos.fdc3.api.types.AppIdentifier;
import com.finos.fdc3.api.types.IntentHandler;
import com.finos.fdc3.api.types.Listener;

/**
 * Interface for intent-related operations.
 */
public interface IntentSupport {

    /**
     * Find applications that can handle a specific intent.
     *
     * @param intent     the intent name
     * @param context    optional context to filter by
     * @param resultType optional result type to filter by
     * @return a CompletionStage containing the app intent information
     */
    CompletionStage<AppIntent> findIntent(String intent, Context context, String resultType);

    /**
     * Find intents that can handle a specific context.
     *
     * @param context the context to find intents for
     * @return a CompletionStage containing the list of app intents
     */
    CompletionStage<List<AppIntent>> findIntentsByContext(Context context);

    /**
     * Raise an intent.
     *
     * @param intent  the intent name
     * @param context the context to pass
     * @param app     optional target application
     * @return a CompletionStage containing the intent resolution
     */
    CompletionStage<IntentResolution> raiseIntent(String intent, Context context, AppIdentifier app);

    /**
     * Raise an intent for a context.
     *
     * @param context the context
     * @param app     optional target application
     * @return a CompletionStage containing the intent resolution
     */
    CompletionStage<IntentResolution> raiseIntentForContext(Context context, AppIdentifier app);

    /**
     * Add an intent listener.
     *
     * @param intent  the intent to listen for
     * @param handler the intent handler
     * @return a CompletionStage containing the listener
     */
    CompletionStage<Listener> addIntentListener(String intent, IntentHandler handler);
}

