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

package org.finos.fdc3.proxy;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.proxy.listeners.RegisterableListener;
import org.finos.fdc3.schema.SchemaConverter;

/**
 * Interface for messaging between the app and the Desktop Agent.
 */
public interface Messaging {

    /**
     * Creates UUIDs used in outgoing messages.
     *
     * @return a new UUID string
     */
    String createUUID();

    /**
     * Post an outgoing message.
     *
     * @param message the message to send
     * @return a CompletionStage that completes when the message is sent
     */
    CompletionStage<Void> post(Map<String, Object> message);

    /**
     * Registers a listener for incoming messages.
     *
     * @param listener the listener to register
     */
    void register(RegisterableListener listener);

    /**
     * Unregister a listener with the given id.
     *
     * @param id the listener id
     */
    void unregister(String id);

    /**
     * Create a metadata element to attach to outgoing messages.
     *
     * @return the metadata map
     */
    Map<String, Object> createMeta();

    /**
     * Waits for a specific matching message.
     *
     * @param <X>                 the expected response type
     * @param filter              predicate to match the expected message
     * @param timeoutMs           timeout in milliseconds
     * @param timeoutErrorMessage error message if timeout occurs
     * @return a CompletionStage containing the matched message
     */
    <X> CompletionStage<X> waitFor(Predicate<X> filter, long timeoutMs, String timeoutErrorMessage);

    /**
     * Sends a request message and waits for a response.
     * If the response contains a payload.error, it is thrown.
     *
     * @param <X>              the expected response type
     * @param message          the request message to send
     * @param expectedTypeName the expected response type name
     * @param timeoutMs        timeout in milliseconds
     * @return a CompletionStage containing the response
     */
    <X> CompletionStage<X> exchange(Map<String, Object> message, String expectedTypeName, long timeoutMs);

    /**
     * App identification used to provide source information used in
     * message meta elements, IntentResolution etc.
     *
     * @return the app identifier
     */
    AppIdentifier getAppIdentifier();

    /**
     * Disconnects the underlying message transport.
     *
     * @return a CompletionStage that completes when disconnected
     */
    CompletionStage<Void> disconnect();

    /**
     * Get the schema converter for converting between JSON and FDC3 message types.
     *
     * @return the SchemaConverter instance
     */
    SchemaConverter getConverter();
}

