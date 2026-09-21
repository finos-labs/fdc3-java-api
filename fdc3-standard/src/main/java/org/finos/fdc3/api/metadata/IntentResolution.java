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

package org.finos.fdc3.api.metadata;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.api.types.IntentResult;

/**
 * IntentResolution provides a standard format for data returned upon resolving an intent.
 * <p>
 * Resolving a "Chain" type intent, where the result is not of interest:
 * <pre>{@code
 * agent.raiseIntent("intentName", context);
 * }</pre>
 * <p>
 * Resolving a "Client-Service" type intent, where the handler returns either data or a
 * {@link org.finos.fdc3.api.channel.Channel}:
 * <pre>{@code
 * agent.raiseIntent("intentName", context)
 *     .thenCompose(resolution -> resolution.getResult()
 *         .thenAccept(result -> {
 *             if (result instanceof Channel) {
 *                 Channel channel = (Channel) result;
 *                 System.out.println(resolution.getSource() + " returned channel " + channel.getId());
 *             } else if (result != null) {
 *                 System.out.println(resolution.getSource() + " returned data: " + result);
 *             } else {
 *                 System.out.println(resolution.getSource() + " did not return data");
 *             }
 *         })
 *         .exceptionally(error -> {
 *             System.out.println(resolution.getSource() + " returned an error: " + error.getMessage());
 *             return null;
 *         }));
 * }</pre>
 * <p>
 * The resolving instance can then be targeted by a further intent, using the identifier this
 * resolution reports as its source:
 * <pre>{@code
 * agent.raiseIntent("intentName", context, resolution.getSource());
 * }</pre>
 */
public interface IntentResolution {
  /**
   * Identifier for the app instance that was selected (or started) to resolve the intent.
   * `source.instanceId` MUST be set, indicating the specific app instance that
   * received the intent.
   */
  AppIdentifier getSource();

  /**
   * The intent that was raised. May be used to determine which intent the user
   * chose in response to `fdc3.raiseIntentForContext()`.
   */
  String getIntent();

  /**
   * Retrieves a promise that will resolve to either `Context` data returned
   * by the application that resolves the raised intent or a `Channel`
   * established and returned by the app resolving the intent.
   *
   * A `Channel` returned will often be of the `PrivateChannel` type. The
   * client can then `addContextListener()` on that channel to, for example,
   * receive a stream of data.
   *
   * The promise MUST reject with a string from the `ResultError` enumeration
   * if an error is thrown by the intent handler, it rejects the returned
   * promise, it does not return a promise or the promise resolves to an
   * object of an invalid type.
   */
  CompletionStage<IntentResult> getResult();

  /**
   * Retrieves metadata about the intent result from the resolving application.
   */
  CompletionStage<ContextMetadata> getResultMetadata();
}
