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

package com.finos.fdc3.proxy.intents;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import com.finos.fdc3.api.metadata.IntentResolution;
import com.finos.fdc3.api.types.AppIdentifier;
import com.finos.fdc3.api.types.IntentResult;
import com.finos.fdc3.proxy.Messaging;

/**
 * Default implementation of IntentResolution.
 */
public class DefaultIntentResolution implements IntentResolution {

    private final Messaging messaging;
    private final CompletionStage<Object> resultPromise;
    private final AppIdentifier source;
    private final String intent;

    public DefaultIntentResolution(
            Messaging messaging,
            CompletionStage<Object> resultPromise,
            AppIdentifier source,
            String intent) {
        this.messaging = messaging;
        this.resultPromise = resultPromise;
        this.source = source;
        this.intent = intent;
    }

    @Override
    public AppIdentifier getSource() {
        return source;
    }

    @Override
    public String getIntent() {
        return intent;
    }

    @Override
    public Optional<String> getVersion() {
        return Optional.empty();
    }

    @Override
    public CompletionStage<IntentResult> getResult() {
        return resultPromise.thenApply(result -> new IntentResult() {
            @Override
            public Object getValue() {
                return result;
            }
        });
    }
}
