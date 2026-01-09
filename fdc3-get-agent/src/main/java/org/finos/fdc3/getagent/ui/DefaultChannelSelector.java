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

package org.finos.fdc3.getagent.ui;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.ui.ChannelSelector;

/**
 * A default no-op implementation of {@link ChannelSelector} for use when channel selection
 * UI is not needed or is handled externally.
 * <p>
 * This implementation does nothing when called - it doesn't display any UI and
 * never triggers the channel change callback. Use this when:
 * <ul>
 *   <li>Your application doesn't use user channels</li>
 *   <li>Channel selection is handled by a different mechanism</li>
 *   <li>You're testing without a UI</li>
 * </ul>
 */
public class DefaultChannelSelector implements ChannelSelector {

    @Override
    public CompletionStage<Void> connect() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> disconnect() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void setChannelChangeCallback(Consumer<String> callback) {
        // No-op: this implementation never changes channels
    }

    @Override
    public void updateChannel(String channelId, List<Channel> availableChannels) {
        // No-op: this implementation doesn't display any UI
    }
}
