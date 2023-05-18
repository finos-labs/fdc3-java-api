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

package com.finos.fdc3.api.channel;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import com.finos.fdc3.api.context.Context;
import com.finos.fdc3.api.metadata.DisplayMetadata;
import com.finos.fdc3.api.types.ContextHandler;
import com.finos.fdc3.api.types.IntentResult;
import com.finos.fdc3.api.types.Listener;

/**
 * Object representing a context channel.
 */
public interface Channel extends IntentResult {
  /**
   * Constant that uniquely identifies this channel.
   */
  String getId();

  /**
   * Uniquely defines each channel type.
   * Can be "user", "app" or "private".
   */
  enum Type {
    User,
    App,
    Private,
  }

  Type getType();

  /**
   * Channels may be visualized and selectable by users. DisplayMetadata may be used to provide hints on how to see them.
   * For App channels, displayMetadata would typically not be present.
   */
  Optional<DisplayMetadata> displayMetadata();

  /**
   * Broadcasts a context on the channel. This function can be used without first joining the channel, allowing applications to broadcast on both App Channels and User Channels that they aren't a member of.
   *
   * If the broadcast is denied by the channel or the channel is not available, the promise will be rejected with an `Error` with a `message` string from the `ChannelError` enumeration.
   *
   * Channel implementations should ensure that context messages broadcast by an application on a channel should not be delivered back to that same application if they are joined to the channel.
   *
   * If you are working with complex context types composed of other simpler types (as recommended by the FDC3 Context Data specification) then you should broadcast each individual type (starting with the simpler types, followed by the complex type) that you want other apps to be able to respond to. Doing so allows applications to filter the context types they receive by adding listeners for specific context types.
   */
  CompletionStage<Void> broadcast(Context context);

  /**
   * When a `contextType`_` is provided, the most recent context matching the type will be returned, or `null` if no matching context is found.
   *
   * If no `contextType` is provided, the most recent context that was broadcast on the channel - regardless of type - will be returned.  If no context has been set on the channel, it will return `null`.
   *
   * It is up to the specific Desktop Agent implementation whether and how recent contexts are stored. For example, an implementation could store context history for a channel in a single array and search through the array for the last context matching a provided type, or context could be maintained as a dictionary keyed by context types. An implementation could also choose not to support context history, in which case this method will return `null` for any context type not matching the type of the most recent context.
   *
   * If getting the current context fails, the promise will be rejected with an `Error` with a `message` string from the `ChannelError` enumeration.
   */
  CompletionStage<Optional<Context>> getCurrentContext();
  
  CompletionStage<Optional<Context>> getCurrentContext(String contextType);

  /**
   * Adds a listener for incoming contexts of the specified _context type_ whenever a broadcast happens on this channel.
   *
   * If, when this function is called, the channel already contains context that would be passed to the listener it is NOT called or passed this context automatically (this behavior differs from that of the [`fdc3.addContextListener`](DesktopAgent#addcontextlistener) function). Apps wishing to access to the current context of the channel should instead call the `getCurrentContext(contextType)` function.
   *
   * Optional metadata about each context message received, including the app that originated the message, SHOULD be provided by the desktop agent implementation.
   */
  CompletionStage<Listener> addContextListener(String contextType, ContextHandler handler);
}
