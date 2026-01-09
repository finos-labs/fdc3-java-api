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

package org.finos.fdc3.api.ui;

import org.finos.fdc3.api.types.AppIdentifier;

/**
 * Represents the user's choice from an intent resolver.
 * <p>
 * This is returned by {@link IntentResolver#chooseIntent} when the user
 * selects an application to handle an intent.
 */
public class IntentResolutionChoice {

    private final String intent;
    private final AppIdentifier appId;

    /**
     * Creates a new IntentResolutionChoice.
     *
     * @param intent the intent that was chosen
     * @param appId the application identifier chosen to handle the intent
     */
    public IntentResolutionChoice(String intent, AppIdentifier appId) {
        this.intent = intent;
        this.appId = appId;
    }

    /**
     * Gets the intent that was chosen.
     *
     * @return the intent name
     */
    public String getIntent() {
        return intent;
    }

    /**
     * Gets the application identifier chosen to handle the intent.
     *
     * @return the app identifier
     */
    public AppIdentifier getAppId() {
        return appId;
    }
}
