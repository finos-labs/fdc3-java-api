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

import java.util.concurrent.CompletionStage;

/**
 * Interface for objects that support connection lifecycle.
 * <p>
 * This is used by UI components like {@link ChannelSelector} and {@link IntentResolver}
 * that may need to connect to external resources.
 */
public interface Connectable {

    /**
     * Connect to the underlying resource.
     *
     * @return a CompletionStage that completes when connected
     */
    CompletionStage<Void> connect();

    /**
     * Disconnect from the underlying resource.
     *
     * @return a CompletionStage that completes when disconnected
     */
    CompletionStage<Void> disconnect();
}
