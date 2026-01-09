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

package org.finos.fdc3.proxy.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.ui.ChannelSelector;
import org.finos.fdc3.testing.world.PropsWorld;

/**
 * A simple channel selector for testing purposes.
 * <p>
 * This selector stores channel state in the PropsWorld for verification.
 * <p>
 * This is equivalent to the TypeScript SimpleChannelSelector class.
 */
public class SimpleChannelSelector implements ChannelSelector {

    public static final String CHANNEL_STATE = "CHANNEL_STATE";

    private final PropsWorld world;
    private Consumer<String> channelChangeCallback;
    private String channelId;
    private List<Channel> channels = new ArrayList<>();

    public SimpleChannelSelector(PropsWorld world) {
        this.world = world;
    }

    @Override
    public void updateChannel(String channelId, List<Channel> availableChannels) {
        this.channelId = channelId;
        this.channels = new ArrayList<>(availableChannels);
        world.set("channelId", channelId);
        world.set("channels", availableChannels);
    }

    @Override
    public void setChannelChangeCallback(Consumer<String> callback) {
        this.channelChangeCallback = callback;
    }

    /**
     * Simulate a channel change (for testing).
     *
     * @param channelId the new channel ID, or null to leave channel
     */
    public void selectChannel(String channelId) {
        this.channelId = channelId;
        if (channelChangeCallback != null) {
            channelChangeCallback.accept(channelId);
        } else {
            throw new IllegalStateException("Channel selected before Channel Change callback was set!");
        }
    }

    public void selectFirstChannel() {
        if (!channels.isEmpty()) {
            selectChannel(channels.get(0).getId());
        }
    }

    public void selectSecondChannel() {
        if (channels.size() > 1) {
            selectChannel(channels.get(1).getId());
        }
    }

    public void deselectChannel() {
        selectChannel(null);
    }

    public String getChannelId() {
        return channelId;
    }

    public List<Channel> getChannels() {
        return channels;
    }

    @Override
    public CompletionStage<Void> connect() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> disconnect() {
        return CompletableFuture.completedFuture(null);
    }
}

