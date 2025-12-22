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

package org.finos.fdc3.api.channel;

import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.types.EventHandler;
import org.finos.fdc3.api.types.Listener;

/**
 * Object representing a private context channel, which is intended to support secure communication between applications, and
 * extends the Channel interface with event handlers which provide information on the connection state of both parties, ensuring
 * that desktop agents do not need to queue or retain messages that are broadcast before a context listener is added and that
 * applications are able to stop broadcasting messages when the other party has disconnected. It is intended that Desktop Agent
 * implementations: - SHOULD restrict external apps from listening or publishing on this channel. - MUST prevent private channels
 * from being retrieved via fdc3.getOrCreateChannel. - MUST provide the `id` value for the channel as required by the Channel
 * interface.
 */
public interface PrivateChannel extends Channel {
    /**
     * Register a handler for events from the PrivateChannel. Whenever the handler function
     * is called it will be passed an event object with details related to the event.
     * 
     * <pre>
     * // any event type
     * Listener listener = await myPrivateChannel.addEventListener(null, event -> {
     *   System.out.println("Received event " + event.getType() + "\n\tDetails: " + event.getDetails());
     * }).toCompletableFuture().join();
     * 
     * // listener for a specific event type
     * Listener channelChangedListener = await myPrivateChannel.addEventListener(
     *    "addContextListener",
     *    event -> { ... }
     * ).toCompletableFuture().join();
     * </pre>
     * 
     * @param type If non-null, only events of the specified type will be received by the handler.
     *             Valid types are: "addContextListener", "unsubscribe", "disconnect", or null for all events.
     * @param handler A function that events received will be passed to.
     * @return A CompletionStage that resolves to a Listener object when the listener is successfully registered.
     */
    CompletionStage<Listener> addEventListener(String type, EventHandler handler);

    /**
     * Adds a listener that will be called each time that the remote app invokes addContextListener on this channel. Desktop Agents
     * MUST call this for each invocation of addContextListener on this channel, including those that occurred before this handler
     * was registered (to prevent race conditions).
     * 
     * @deprecated Use {@link #addEventListener(String, EventHandler)} instead
     */
    @Deprecated
    CompletionStage<Listener> onAddContextListener(EventHandler handler);

    /**
     * Adds a listener that will be called whenever the remote app invokes Listener.unsubscribe() on a context listener that it
     * previously added. Desktop Agents MUST call this when disconnect() is called by the other party, for each listener that they
     * have added.
     * 
     * @deprecated Use {@link #addEventListener(String, EventHandler)} instead
     */
    @Deprecated
    CompletionStage<Listener> onUnsubscribe(EventHandler handler);

    /**
     * Adds a listener that will be called when the remote app terminates, for example when its window is closed or because
     * disconnect was called. This is in addition to calls that will be made to onUnsubscribe listeners.
     * 
     * @deprecated Use {@link #addEventListener(String, EventHandler)} instead
     */
    @Deprecated
    CompletionStage<Listener> onDisconnect(EventHandler handler);

    /**
     * May be called to indicate that a participant will no longer interact with this channel. After this function has been called,
     * Desktop Agents SHOULD prevent apps from broadcasting on this channel and MUST automatically call Listener.unsubscribe() for
     * each listener that they've added (causing any onUnsubscribe handler added by the other party to be called) before triggering
     * any onDisconnect handler added by the other party.
     */
    void disconnect();
    
    @Override
    default Type getType() {

        return Type.Private;
    }
}
