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

import java.util.List;
import java.util.function.Consumer;

import org.finos.fdc3.api.channel.Channel;

/**
 * Interface used by the desktop agent proxy to handle the channel selection process.
 * <p>
 * Implementations of this interface provide a UI for users to select which user channel
 * they want to join. The Desktop Agent proxy will call {@link #updateChannel} when
 * the channel list or current channel changes, and the implementation should call
 * the callback set via {@link #setChannelChangeCallback} when the user selects a channel.
 */
public interface ChannelSelector extends Connectable {

    /**
     * Set the callback to be invoked when the user selects a different channel.
     *
     * @param callback the callback, receiving the channel ID or null if leaving all channels
     */
    void setChannelChangeCallback(Consumer<String> callback);

    /**
     * Called when the list of user channels is updated, or the selected channel changes.
     *
     * @param channelId the current channel ID, or null if no channel is selected
     * @param availableChannels the list of available user channels
     */
    void updateChannel(String channelId, List<Channel> availableChannels);
}
