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

package org.finos.fdc3.getagent.ui;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.errors.ResolveError;
import org.finos.fdc3.api.metadata.AppIntent;
import org.finos.fdc3.api.metadata.AppMetadata;
import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.api.ui.IntentResolutionChoice;
import org.finos.fdc3.api.ui.IntentResolver;

/**
 * A configurable default implementation of {@link IntentResolver} for use when intent resolution
 * UI is not needed or is handled externally.
 * <p>
 * The behavior can be configured using {@link ResolverBehavior}:
 * <ul>
 *   <li>{@link ResolverBehavior#USE_FIRST} - Automatically selects the first intent and first app</li>
 *   <li>{@link ResolverBehavior#CANCEL} - Returns null (indicating user cancellation)</li>
 *   <li>{@link ResolverBehavior#THROW_ERROR} - Throws a ResolveError</li>
 * </ul>
 */
public class DefaultIntentResolver implements IntentResolver {

    /**
     * Defines the behavior of the DefaultIntentResolver when asked to resolve an intent.
     */
    public enum ResolverBehavior {
        /**
         * Automatically select the first intent and first application.
         * This is useful for testing or when only one option is expected.
         */
        USE_FIRST,

        /**
         * Return null, indicating the user cancelled the resolution.
         * This will cause the raiseIntent call to fail with UserCancelled.
         */
        CANCEL,

        /**
         * Throw an error indicating no resolver is available.
         * This will cause the raiseIntent call to fail with ResolverUnavailable.
         */
        THROW_ERROR
    }

    private final ResolverBehavior behavior;

    /**
     * Creates a DefaultIntentResolver with the default behavior of {@link ResolverBehavior#USE_FIRST}.
     */
    public DefaultIntentResolver() {
        this(ResolverBehavior.USE_FIRST);
    }

    /**
     * Creates a DefaultIntentResolver with the specified behavior.
     *
     * @param behavior the behavior to use when resolving intents
     */
    public DefaultIntentResolver(ResolverBehavior behavior) {
        this.behavior = behavior != null ? behavior : ResolverBehavior.USE_FIRST;
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
    public CompletionStage<IntentResolutionChoice> chooseIntent(List<AppIntent> appIntents, Context context) {
        switch (behavior) {
            case USE_FIRST:
                return resolveWithFirst(appIntents);

            case CANCEL:
                return CompletableFuture.completedFuture(null);

            case THROW_ERROR:
                CompletableFuture<IntentResolutionChoice> future = new CompletableFuture<>();
                future.completeExceptionally(new RuntimeException(ResolveError.ResolverUnavailable.toString()));
                return future;

            default:
                return CompletableFuture.completedFuture(null);
        }
    }

    private CompletionStage<IntentResolutionChoice> resolveWithFirst(List<AppIntent> appIntents) {
        if (appIntents == null || appIntents.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        AppIntent firstIntent = appIntents.get(0);
        if (firstIntent.getIntent() == null || firstIntent.getApps() == null || firstIntent.getApps().length == 0) {
            return CompletableFuture.completedFuture(null);
        }

        List<AppMetadata> apps = Arrays.asList(firstIntent.getApps());
        AppMetadata firstApp = apps.get(0);

        AppIdentifier appIdentifier = new AppIdentifier(
                firstApp.getAppId(),
                firstApp.getInstanceId(),
                firstApp.getDesktopAgent()
        );

        IntentResolutionChoice choice = new IntentResolutionChoice(
                firstIntent.getIntent().getName(),
                appIdentifier
        );

        return CompletableFuture.completedFuture(choice);
    }

    /**
     * Gets the behavior configured for this resolver.
     *
     * @return the resolver behavior
     */
    public ResolverBehavior getBehavior() {
        return behavior;
    }
}
