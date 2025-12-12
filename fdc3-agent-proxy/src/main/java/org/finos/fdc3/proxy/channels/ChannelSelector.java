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

package org.finos.fdc3.proxy.channels;

import java.util.List;
import java.util.function.Consumer;

import org.finos.fdc3.api.channel.Channel;

/**
 * Interface for channel selection UI components.
 */
public interface ChannelSelector {

    /**
     * Set the callback to be invoked when the user selects a different channel.
     *
     * @param callback the callback, receiving the channel ID or null if leaving
     */
    void setChannelChangeCallback(Consumer<String> callback);

    /**
     * Update the current channel and available channels.
     *
     * @param channelId the current channel ID, or null if none
     * @param channels  the available channels
     */
    void updateChannel(String channelId, List<Channel> channels);
}

