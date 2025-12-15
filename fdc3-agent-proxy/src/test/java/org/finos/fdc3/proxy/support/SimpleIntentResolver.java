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

package org.finos.fdc3.proxy.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.metadata.AppIntent;
import org.finos.fdc3.api.metadata.AppMetadata;
import org.finos.fdc3.api.metadata.IntentMetadata;
import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.proxy.intents.IntentResolutionChoice;
import org.finos.fdc3.proxy.intents.IntentResolver;
import org.finos.fdc3.testing.world.PropsWorld;

/**
 * A simple intent resolver for testing purposes.
 * <p>
 * This resolver automatically selects the first intent/app in the list,
 * unless the context type is "fdc3.cancel-me", in which case it returns null (cancelled).
 * <p>
 * This is equivalent to the TypeScript SimpleIntentResolver class.
 */
public class SimpleIntentResolver implements IntentResolver {

    private final PropsWorld world;

    public SimpleIntentResolver(PropsWorld world) {
        this.world = world;
    }

    @Override
    public CompletionStage<IntentResolutionChoice> chooseIntent(List<AppIntent> appIntents, Context context) {
        // Cancel if the context type is "fdc3.cancel-me"
        if ("fdc3.cancel-me".equals(context.getType())) {
            return CompletableFuture.completedFuture(null);
        }

        // Select the first intent and first app
        AppIntent firstIntent = appIntents.get(0);
        IntentMetadata intent = firstIntent.getIntent();
        List<AppMetadata> apps = new ArrayList<>(firstIntent.getApps());
        AppMetadata firstApp = apps.get(0);

        // Create an AppIdentifier from the AppMetadata
        final String appId = firstApp.getAppId();
        final Optional<String> instanceId = firstApp.getInstanceId();
        AppIdentifier appIdentifier = new AppIdentifier() {
            @Override
            public String getAppId() {
                return appId;
            }

            @Override
            public Optional<String> getInstanceId() {
                return instanceId;
            }
        };

        // Create the resolution choice
        IntentResolutionChoice resolution = new IntentResolutionChoice(
                intent.getName(),
                appIdentifier
        );

        // Store for testing verification
        world.set("intent-resolution", resolution);

        return CompletableFuture.completedFuture(resolution);
    }
}

