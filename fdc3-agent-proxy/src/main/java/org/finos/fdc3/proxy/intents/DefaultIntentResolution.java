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

package org.finos.fdc3.proxy.intents;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.metadata.ContextMetadata;
import org.finos.fdc3.api.metadata.IntentResolution;
import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.api.types.IntentResult;
import org.finos.fdc3.proxy.Messaging;

/**
 * Default implementation of IntentResolution.
 */
public class DefaultIntentResolution implements IntentResolution {

    private final Messaging messaging;
    private final long messageExchangeTimeout;
    private final CompletionStage<IntentResult> resultPromise;
    private final CompletionStage<ContextMetadata> resultMetadataPromise;
    private final AppIdentifier source;
    private final String intent;

    public DefaultIntentResolution(
            Messaging messaging,
            long messageExchangeTimeout,
            CompletionStage<IntentResult> resultPromise,
            CompletionStage<ContextMetadata> resultMetadataPromise,
            AppIdentifier source,
            String intent) {
        this.messaging = messaging;
        this.messageExchangeTimeout = messageExchangeTimeout;
        this.resultPromise = resultPromise;
        this.resultMetadataPromise = resultMetadataPromise;
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
        return resultPromise;
    }

    @Override
    public CompletionStage<ContextMetadata> getResultMetadata() {
        return resultMetadataPromise;
    }
}
