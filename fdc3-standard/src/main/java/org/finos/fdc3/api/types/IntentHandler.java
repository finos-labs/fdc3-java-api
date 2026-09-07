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

package org.finos.fdc3.api.types;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.metadata.ContextMetadata;

/**
 * Callback invoked when an intent is raised to this application.
 * <p>
 * Registered via {@link org.finos.fdc3.api.DesktopAgent#addIntentListener(String, IntentHandler)}
 * (and related overloads). The handler receives the raised context and metadata about the
 * message, and may return a result to the raising application through
 * {@link org.finos.fdc3.api.metadata.IntentResolution}.
 */
@FunctionalInterface
public interface IntentHandler {

    /**
     * Handles a raised intent.
     *
     * @param context         the context object supplied with the raised intent
     * @param contextMetadata metadata for the raised intent (originating app, timestamp, and any
     *                        app-providable fields forwarded by the Desktop Agent); never
     *                        {@code null}, though individual fields may be absent
     * @return a {@link CompletionStage} that completes with an optional result for the raising
     *         app. Use {@link Optional#empty()} (or complete with {@code null} inside the
     *         optional) when there is no result. A present value SHOULD be one of:
     *         <ul>
     *           <li>{@link Context} — context data returned to the raiser via
     *               {@link org.finos.fdc3.api.metadata.IntentResolution#getResult()}</li>
     *           <li>{@link ContextWithMetadata} — context plus app-providable metadata; the
     *               Desktop Agent merges that metadata and exposes it via
     *               {@link org.finos.fdc3.api.metadata.IntentResolution#getResultMetadata()},
     *               while {@code getResult()} returns only the {@link Context}</li>
     *           <li>{@link org.finos.fdc3.api.channel.Channel} or
     *               {@link org.finos.fdc3.api.channel.PrivateChannel} — a channel over which
     *               further responses will be streamed</li>
     *         </ul>
     *         If the stage completes exceptionally, the raiser's {@code getResult()} promise
     *         is rejected with {@link org.finos.fdc3.api.errors.ResultError#IntentHandlerRejected}.
     */
    CompletionStage<Optional<Object>> handleIntent(Context context, ContextMetadata contextMetadata);

}
