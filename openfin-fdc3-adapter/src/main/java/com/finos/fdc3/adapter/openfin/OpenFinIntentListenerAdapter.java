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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finos.fdc3.api.context.Context;
import com.finos.fdc3.api.types.IntentHandler;
import com.finos.fdc3.api.types.IntentResult;
import com.finos.fdc3.api.utils.JacksonUtilities;
import com.openfin.desktop.interop.Intent;
import com.openfin.desktop.interop.IntentListener;

/**
 * Adapter to convert FINOS {@link IntentHandler} to OpenFin {@link IntentListener}
 * 
 * @author Tim Jenkel
 */
public class OpenFinIntentListenerAdapter implements IntentListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenFinIntentListenerAdapter.class);

    private final IntentHandler handler;

    public OpenFinIntentListenerAdapter(IntentHandler handler) {

        this.handler = handler;
    }

    @Override
    public void onIntent(Intent intent) {

        Context fdc3Context;
        try {
            // TODO deserialize to correct class based on context type
            fdc3Context = JacksonUtilities.deserializeFromString(intent.getContext().toString(), Context.class);
        } catch (Exception e) {
            LOGGER.error("Error deserializing intent context: {}", intent, e);
            return;
        }
        Optional<CompletionStage<IntentResult>> result = handler.handleIntent(fdc3Context, null);
        if (result.isPresent()) {
            LOGGER.warn("Ignoring intent result not supported by OpenFin");
        }
    }

}
