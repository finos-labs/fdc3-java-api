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

package com.finos.fdc3.adapter.openfin;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import com.finos.fdc3.api.channel.Channel;
import com.finos.fdc3.api.context.Context;
import com.finos.fdc3.api.metadata.DisplayMetadata;
import com.finos.fdc3.api.types.ContextHandler;
import com.finos.fdc3.api.types.Listener;
import com.openfin.desktop.interop.ContextGroupInfo;

/**
 * OpenFin implementation of FDC3 {@link Channel}
 * 
 * @author Tim Jenkel
 */
public class OpenFinChannel implements Channel {

    private final ContextGroupInfo info;

    private final Type type;

    private final OpenFinDesktopAgent desktopAgent;

    public OpenFinChannel(ContextGroupInfo info, Type type, OpenFinDesktopAgent desktopAgent) {

        this.info = info;
        this.type = type;
        this.desktopAgent = desktopAgent;
    }

    @Override
    public String getId() {

        return info.getId();
    }

    @Override
    public Type getType() {

        return type;
    }

    @Override
    public Optional<DisplayMetadata> displayMetadata() {

        return Optional.ofNullable(info.getDisplayMetadata()).map(OpenFinChannelDisplayMetadata::new);
    }

    @Override
    public CompletionStage<Void> broadcast(Context context) {

        return desktopAgent.broadcastOnChannel(context, getId());
    }

    @Override
    public CompletionStage<Optional<Context>> getCurrentContext() {

        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Optional<Context>> getCurrentContext(String contextType) {

        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Listener> addContextListener(String contextType, ContextHandler handler) {

        throw new UnsupportedOperationException(
            "Channel.addContextListener is not supported, please use DesktopAgent.addContextListener");
    }

}
