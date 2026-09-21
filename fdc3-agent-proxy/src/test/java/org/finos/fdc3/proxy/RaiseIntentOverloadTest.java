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

package org.finos.fdc3.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.finos.fdc3.api.DesktopAgent;
import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.metadata.AppIntent;
import org.finos.fdc3.api.metadata.AppProvidableContextMetadata;
import org.finos.fdc3.api.metadata.ContextMetadata;
import org.finos.fdc3.api.metadata.IntentResolution;
import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.api.types.IntentHandler;
import org.finos.fdc3.api.types.Listener;
import org.finos.fdc3.proxy.intents.IntentSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the arity of the {@code raiseIntent} and {@code raiseIntentForContext} overload sets.
 * <p>
 * TypeScript declares each of these as a single method with trailing optional parameters. Java
 * has to express that as overloads, and an earlier arrangement offered two different methods at
 * the same arity - for example {@code (intent, context, app)} alongside
 * {@code (intent, context, metadata)} - which made {@code raiseIntent("i", context, null)}
 * ambiguous and impossible to compile.
 * <p>
 * The calls in {@link #callableShapes} therefore matter at compile time as much as at run time:
 * reintroducing a colliding overload would stop this class compiling. The assertions additionally
 * confirm that each shorter form pads the remaining arguments with null from the right, rather
 * than reordering them.
 */
class RaiseIntentOverloadTest {

    /**
     * Records the arguments the proxy forwards to the intent layer, so the tests can assert on
     * how each default overload padded its call.
     */
    private static final class RecordingIntentSupport implements IntentSupport {

        private String intent;
        private Context context;
        private AppIdentifier app;
        private Boolean newInstance;
        private AppProvidableContextMetadata metadata;
        private int callCount;

        @Override
        public CompletionStage<IntentResolution> raiseIntent(
                String intent,
                Context context,
                AppIdentifier app,
                Boolean newInstance,
                AppProvidableContextMetadata metadata) {
            this.intent = intent;
            this.context = context;
            this.app = app;
            this.newInstance = newInstance;
            this.metadata = metadata;
            this.callCount++;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<IntentResolution> raiseIntentForContext(
                Context context,
                AppIdentifier app,
                Boolean newInstance,
                AppProvidableContextMetadata metadata) {
            this.intent = null;
            this.context = context;
            this.app = app;
            this.newInstance = newInstance;
            this.metadata = metadata;
            this.callCount++;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<AppIntent> findIntent(String intent, Context context, String resultType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletionStage<List<AppIntent>> findIntentsByContext(Context context, String resultType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletionStage<Listener> addIntentListener(String intent, IntentHandler handler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletionStage<Listener> addIntentListenerWithContext(
                String intent, List<String> contextTypes, IntentHandler handler) {
            throw new UnsupportedOperationException();
        }
    }

    private RecordingIntentSupport intents;
    private DesktopAgent agent;

    private DesktopAgent agent() {
        if (agent == null) {
            intents = new RecordingIntentSupport();
            agent = new DesktopAgentProxy(null, null, intents, null, List.of());
        }
        return agent;
    }

    /**
     * Every call shape the interface offers. This method is never invoked; it exists so that the
     * compiler resolves each shape, which is what regressed previously.
     */
    @SuppressWarnings("unused")
    private static void callableShapes(DesktopAgent agent, Context context, AppIdentifier app,
            AppProvidableContextMetadata metadata) {
        agent.raiseIntent("StartChat", context);
        agent.raiseIntent("StartChat", context, app);
        agent.raiseIntent("StartChat", context, app, true);
        agent.raiseIntent("StartChat", context, app, true, metadata);

        // The shape that did not compile before the overloads were collapsed.
        agent.raiseIntent("StartChat", context, null);
        agent.raiseIntent("StartChat", context, null, null, metadata);

        agent.raiseIntentForContext(context);
        agent.raiseIntentForContext(context, app);
        agent.raiseIntentForContext(context, app, true);
        agent.raiseIntentForContext(context, app, true, metadata);

        agent.raiseIntentForContext(context, null);
        agent.raiseIntentForContext(context, null, null, metadata);
    }

    @Test
    @DisplayName("raiseIntent with an explicitly null app resolves to a single overload")
    void nullAppIsUnambiguous() {
        Context context = new Context("fdc3.instrument");

        agent().raiseIntent("StartChat", context, null);

        assertEquals(1, intents.callCount);
        assertEquals("StartChat", intents.intent);
        assertSame(context, intents.context);
        assertNull(intents.app);
        assertNull(intents.newInstance);
        assertNull(intents.metadata);
    }

    @Test
    @DisplayName("Shorter raiseIntent overloads pad the trailing arguments with null")
    void shorterOverloadsPadFromTheRight() {
        Context context = new Context("fdc3.instrument");
        AppIdentifier app = new AppIdentifier("target-app");

        agent().raiseIntent("StartChat", context);
        assertNull(intents.app);
        assertNull(intents.newInstance);
        assertNull(intents.metadata);

        agent().raiseIntent("StartChat", context, app);
        assertSame(app, intents.app);
        assertNull(intents.newInstance);
        assertNull(intents.metadata);

        agent().raiseIntent("StartChat", context, app, true);
        assertSame(app, intents.app);
        assertEquals(Boolean.TRUE, intents.newInstance);
        assertNull(intents.metadata);
    }

    @Test
    @DisplayName("Metadata is only reachable through the full-arity raiseIntent")
    void metadataReachesTheIntentLayer() {
        Context context = new Context("fdc3.instrument");
        AppProvidableContextMetadata metadata = new ContextMetadata();

        agent().raiseIntent("StartChat", context, null, null, metadata);

        assertSame(metadata, intents.metadata);
        assertNull(intents.app);
        assertNull(intents.newInstance);
    }

    @Test
    @DisplayName("raiseIntentForContext overloads pad the trailing arguments with null")
    void raiseIntentForContextPadsFromTheRight() {
        Context context = new Context("fdc3.instrument");
        AppIdentifier app = new AppIdentifier("target-app");

        agent().raiseIntentForContext(context);
        assertSame(context, intents.context);
        assertNull(intents.app);
        assertNull(intents.newInstance);
        assertNull(intents.metadata);

        agent().raiseIntentForContext(context, app, true);
        assertSame(app, intents.app);
        assertEquals(Boolean.TRUE, intents.newInstance);
        assertNull(intents.metadata);
    }
}
