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

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import com.finos.fdc3.api.channel.Channel;

/**
 * Interface for selecting channels.
 * <p>
 * Implementations of this interface handle the user interaction
 * for selecting a channel from available options.
 */
public interface ChannelSelector {

    /**
     * Update the current channel state.
     *
     * @param channelId         the current channel ID, or null if not joined
     * @param availableChannels the list of available channels
     * @return a CompletionStage that completes when updated
     */
    CompletionStage<Void> updateChannel(String channelId, List<Channel> availableChannels);

    /**
     * Set a callback to be invoked when the user changes channels.
     *
     * @param callback the callback, accepting the new channel ID (or null to leave)
     */
    void setChannelChangeCallback(Consumer<String> callback);

    /**
     * Connect the selector (e.g., initialize UI components).
     *
     * @return a CompletionStage that completes when connected
     */
    CompletionStage<Void> connect();

    /**
     * Disconnect the selector (e.g., cleanup UI components).
     *
     * @return a CompletionStage that completes when disconnected
     */
    CompletionStage<Void> disconnect();
}

