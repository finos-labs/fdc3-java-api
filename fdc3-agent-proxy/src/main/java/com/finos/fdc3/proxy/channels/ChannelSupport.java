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

package com.finos.fdc3.proxy.channels;

import java.util.List;
import java.util.concurrent.CompletionStage;

import com.finos.fdc3.api.channel.Channel;
import com.finos.fdc3.api.channel.PrivateChannel;
import com.finos.fdc3.api.types.ContextHandler;
import com.finos.fdc3.api.types.EventHandler;
import com.finos.fdc3.api.types.Listener;

/**
 * Interface for channel-related operations.
 */
public interface ChannelSupport {

    /**
     * Get the current user channel.
     *
     * @return a CompletionStage containing the current channel, or null if not joined
     */
    CompletionStage<Channel> getUserChannel();

    /**
     * Get all available user channels.
     *
     * @return a CompletionStage containing the list of user channels
     */
    CompletionStage<List<Channel>> getUserChannels();

    /**
     * Get or create an app channel with the specified ID.
     *
     * @param id the channel ID
     * @return a CompletionStage containing the channel
     */
    CompletionStage<Channel> getOrCreate(String id);

    /**
     * Create a new private channel.
     *
     * @return a CompletionStage containing the new private channel
     */
    CompletionStage<PrivateChannel> createPrivateChannel();

    /**
     * Leave the current user channel.
     *
     * @return a CompletionStage that completes when left
     */
    CompletionStage<Void> leaveUserChannel();

    /**
     * Join a user channel by ID.
     *
     * @param id the channel ID to join
     * @return a CompletionStage that completes when joined
     */
    CompletionStage<Void> joinUserChannel(String id);

    /**
     * Add a context listener.
     *
     * @param handler the context handler
     * @param type    the context type to listen for, or null for all types
     * @return a CompletionStage containing the listener
     */
    CompletionStage<Listener> addContextListener(ContextHandler handler, String type);

    /**
     * Add an event listener.
     *
     * @param handler the event handler
     * @param type    the event type to listen for, or null for all types
     * @return a CompletionStage containing the listener
     */
    CompletionStage<Listener> addEventListener(EventHandler handler, String type);
}

