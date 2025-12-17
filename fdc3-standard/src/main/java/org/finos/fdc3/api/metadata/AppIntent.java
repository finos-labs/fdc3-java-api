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

package org.finos.fdc3.api.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An interface that relates an intent to apps.
 *
 * Used if a raiseIntent request requires additional resolution (e.g. by showing an intent
 * resolver) before it can be handled.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppIntent {

    private AppMetadata[] apps;
    private IntentMetadata intent;

    /**
     * Default constructor for Jackson deserialization.
     */
    public AppIntent() {
    }

    /**
     * Details of applications that can resolve the intent.
     */
    @JsonProperty("apps")
    public AppMetadata[] getApps() {
        return apps;
    }

    public void setApps(AppMetadata[] apps) {
        this.apps = apps;
    }

    /**
     * Details of the intent whose relationship to resolving applications is being described.
     */
    @JsonProperty("intent")
    public IntentMetadata getIntent() {
        return intent;
    }

    public void setIntent(IntentMetadata intent) {
        this.intent = intent;
    }
}
