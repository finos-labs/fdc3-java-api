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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.finos.fdc3.api.context.Context;
import com.finos.fdc3.api.metadata.AppIntent;
import com.finos.fdc3.api.metadata.AppMetadata;
import com.finos.fdc3.api.metadata.IntentMetadata;
import com.finos.fdc3.api.types.IntentResult;
import com.finos.fdc3.testing.world.PropsWorld;

/**
 * A simple intent resolver for testing purposes.
 * <p>
 * This resolver automatically selects the first intent/app in the list,
 * unless the context type is "fdc3.cancel-me", in which case it cancels.
 * <p>
 * This is equivalent to the TypeScript SimpleIntentResolver class.
 */
public class SimpleIntentResolver implements IntentResolver {

    private final PropsWorld world;

    public SimpleIntentResolver(PropsWorld world) {
        this.world = world;
    }

    @Override
    public CompletionStage<Void> connect() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> disconnect() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<IntentResult> intentChosen(IntentResult intentResult) {
        world.set("intent-result", intentResult);
        return CompletableFuture.completedFuture(intentResult);
    }

    @Override
    public CompletionStage<Optional<IntentResolutionChoice>> chooseIntent(
            List<AppIntent> appIntents, Context context) {

        // Cancel if the context type is "fdc3.cancel-me"
        if ("fdc3.cancel-me".equals(context.getType())) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        // Select the first intent and first app
        AppIntent firstIntent = appIntents.get(0);
        IntentMetadata intent = firstIntent.getIntent();
        List<AppMetadata> apps = new ArrayList<>(firstIntent.getApps());
        AppMetadata firstApp = apps.get(0);

        // Store the resolution for testing
        IntentResolutionChoice resolution = new IntentResolutionChoice(
                firstApp.getAppId(),
                firstApp.getInstanceId().orElse(null),
                intent.getName()
        );

        world.set("intent-resolution", resolution);

        return CompletableFuture.completedFuture(Optional.of(resolution));
    }

    /**
     * Represents a choice made during intent resolution.
     */
    public static class IntentResolutionChoice {
        private final String appId;
        private final String instanceId;
        private final String intent;

        public IntentResolutionChoice(String appId, String instanceId, String intent) {
            this.appId = appId;
            this.instanceId = instanceId;
            this.intent = intent;
        }

        public String getAppId() {
            return appId;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public String getIntent() {
            return intent;
        }
    }
}

