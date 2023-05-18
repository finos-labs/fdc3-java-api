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

import java.util.Objects;

import com.finos.fdc3.api.context.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finos.fdc3.api.context.Context;
import com.finos.fdc3.api.types.ContextHandler;
import com.finos.fdc3.api.utils.JacksonUtilities;
import com.openfin.desktop.interop.ContextListener;

/**
 * Adapter that converts FDC3 {@link ContextHandler} to OpenFin {@link ContextListener}
 * 
 * @author Tim Jenkel
 */
public class OpenFinContextListenerAdapter implements ContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenFinContextListenerAdapter.class);

    private final ContextHandler handler;

    public OpenFinContextListenerAdapter(ContextHandler handler) {

        this.handler = Objects.requireNonNull(handler);
    }

    @Override
    public void onContext(com.openfin.desktop.interop.Context openFinContext) {

        Context fdc3Context;
        try {
            // TODO deserialize to correct class based on context type
            if(openFinContext.getType().equalsIgnoreCase("fdc3.position")){
                fdc3Context = JacksonUtilities.deserializeFromString(openFinContext.toString(), Position.class);
            }else{
                fdc3Context = JacksonUtilities.deserializeFromString(openFinContext.toString(), Context.class);
            }

        } catch (Exception e) {
            LOGGER.error("Error deserializing context: {}", openFinContext, e);
            return;
        }
        handler.handleContext(fdc3Context, null);
    }

}
