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

import org.finos.fdc3.api.channel.Channel;
import org.finos.fdc3.api.types.Listener;
import org.finos.fdc3.proxy.listeners.RegisterableListener;

/**
 * This is a special version of a ContextListener created when the user calls the
 * fdc3.addContextListener method. In this scenario, the listener will respond to broadcasts
 * on whatever is the current user channel.
 */
public interface UserChannelContextListener extends Listener, RegisterableListener {

    /**
     * This method is called when the user channel changes. The listener should then
     * call its handler with the latest piece of relevant channel state and start responding to
     * events on the new channelId.
     */
    void changeChannel();
}
