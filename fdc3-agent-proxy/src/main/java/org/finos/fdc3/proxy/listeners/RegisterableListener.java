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

package org.finos.fdc3.proxy.listeners;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.types.Listener;

/**
 * A listener that can be registered with the messaging system.
 */
public interface RegisterableListener extends Listener {

    /**
     * Get the unique identifier for this listener.
     *
     * @return the listener ID
     */
    String getId();

    /**
     * Filter function to determine if a message should be processed.
     *
     * @param message the incoming message
     * @return true if the message should be processed
     */
    boolean filter(Map<String, Object> message);

    /**
     * Action to perform when a matching message is received.
     *
     * @param message the matched message
     */
    void action(Map<String, Object> message);

    /**
     * Register this listener with the messaging system.
     *
     * @return a CompletionStage that completes when registered
     */
    CompletionStage<Void> register();

    /**
     * Unsubscribe this listener from the messaging system.
     * 
     * @return a CompletionStage that completes when unsubscribed
     */
    CompletionStage<Void> unsubscribe();
}

