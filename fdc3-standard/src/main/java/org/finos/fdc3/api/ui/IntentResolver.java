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

package org.finos.fdc3.api.ui;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.metadata.AppIntent;

/**
 * Interface used by the desktop agent proxy to handle the intent resolution process.
 * <p>
 * Implementations of this interface provide a UI for users to choose which application
 * should handle an intent when multiple applications are available.
 */
public interface IntentResolver extends Connectable {

    /**
     * Called when the user needs to resolve an intent.
     * <p>
     * The implementation should display a UI allowing the user to select from the
     * available applications that can handle the intent(s).
     *
     * @param appIntents the available intents and apps that can handle them
     * @param context the context being passed to the intent
     * @return a CompletionStage containing the user's choice, or null if the operation was cancelled
     */
    CompletionStage<IntentResolutionChoice> chooseIntent(List<AppIntent> appIntents, Context context);
}
