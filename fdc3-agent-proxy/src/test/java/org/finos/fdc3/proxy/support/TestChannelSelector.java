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

package org.finos.fdc3.proxy.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.testing.agent.ChannelSelector;

/**
 * Test implementation of ChannelSelector for Cucumber tests.
 */
public class TestChannelSelector implements ChannelSelector {

    private Consumer<String> callback;
    private String channelId;
    private List<Channel> channels = new ArrayList<>();

    @Override
    public CompletionStage<Void> updateChannel(String channelId, List<Channel> availableChannels) {
        this.channelId = channelId;
        this.channels = new ArrayList<>(availableChannels);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void setChannelChangeCallback(Consumer<String> callback) {
        this.callback = callback;
    }

    @Override
    public CompletionStage<Void> connect() {
        System.out.println("TestChannelSelector was connected");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> disconnect() {
        System.out.println("TestChannelSelector was disconnected");
        return CompletableFuture.completedFuture(null);
    }

    public void selectChannel(String channelId) {
        this.channelId = channelId;
        if (callback != null) {
            callback.accept(channelId);
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
}

