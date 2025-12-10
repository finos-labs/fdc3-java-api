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

package com.finos.fdc3.proxy.steps;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.finos.fdc3.api.context.Context;
import com.finos.fdc3.api.metadata.ContextMetadata;
import com.finos.fdc3.api.types.AppIdentifier;
import com.finos.fdc3.proxy.support.TestMessaging;
import com.finos.fdc3.proxy.world.CustomWorld;

import io.cucumber.java.en.Given;

import static com.finos.fdc3.testing.support.MatchingUtils.handleResolve;

/**
 * Cucumber step definitions for intent-related tests.
 */
public class IntentSteps {

    private final CustomWorld world;

    public IntentSteps(CustomWorld world) {
        this.world = world;
    }

    @Given("app {string}")
    public void app(String appStr) {
        String[] parts = appStr.split("/");
        String appId = parts[0];
        String instanceId = parts.length > 1 ? parts[1] : null;

        AppIdentifier app = createAppIdentifier(appId, instanceId);
        world.getMessaging().addAppIntentDetail(createIntentDetail(app, null, null, null));
        if (instanceId != null) {
            world.set(instanceId, app);
        }
    }

    @Given("app {string} resolves intent {string}")
    public void appResolvesIntent(String appStr, String intent) {
        String[] parts = appStr.split("/");
        String appId = parts[0];
        String instanceId = parts.length > 1 ? parts[1] : null;

        AppIdentifier app = createAppIdentifier(appId, instanceId);
        world.getMessaging().addAppIntentDetail(createIntentDetail(app, intent, null, null));
        if (instanceId != null) {
            world.set(instanceId, app);
        }
        world.set(appId, createAppIdentifier(appId, null));
    }

    @Given("app {string} resolves intent {string} with result type {string}")
    public void appResolvesIntentWithResultType(String appStr, String intent, String resultType) {
        String[] parts = appStr.split("/");
        String appId = parts[0];
        String instanceId = parts.length > 1 ? parts[1] : null;

        AppIdentifier app = createAppIdentifier(appId, instanceId);
        world.getMessaging().addAppIntentDetail(createIntentDetail(app, intent, null, resultType));
        if (instanceId != null) {
            world.set(instanceId, app);
        }
        world.set(appId, createAppIdentifier(appId, null));
    }

    @Given("app {string} resolves intent {string} with context {string}")
    public void appResolvesIntentWithContext(String appStr, String intent, String context) {
        String[] parts = appStr.split("/");
        String appId = parts[0];
        String instanceId = parts.length > 1 ? parts[1] : null;

        AppIdentifier app = createAppIdentifier(appId, instanceId);
        world.getMessaging().addAppIntentDetail(createIntentDetail(app, intent, context, null));
        if (instanceId != null) {
            world.set(instanceId, app);
        }
        world.set(appId, createAppIdentifier(appId, null));
    }

    @Given("app {string} resolves intent {string} with context {string} and result type {string}")
    public void appResolvesIntentWithContextAndResultType(String appStr, String intent, String context, String resultType) {
        String[] parts = appStr.split("/");
        String appId = parts[0];
        String instanceId = parts.length > 1 ? parts[1] : null;

        AppIdentifier app = createAppIdentifier(appId, instanceId);
        world.getMessaging().addAppIntentDetail(createIntentDetail(app, intent, context, resultType));
        if (instanceId != null) {
            world.set(instanceId, app);
        }
    }

    @Given("Raise Intent returns a context of {string}")
    public void raiseIntentReturnsContext(String result) {
        TestMessaging.PossibleIntentResult intentResult = new TestMessaging.PossibleIntentResult();
        intentResult.setContext((Context) handleResolve(result, world));
        world.getMessaging().setIntentResult(intentResult);
    }

    @Given("Raise Intent will throw a {string} error")
    public void raiseIntentWillThrowError(String error) {
        TestMessaging.PossibleIntentResult intentResult = new TestMessaging.PossibleIntentResult();
        intentResult.setError(error);
        world.getMessaging().setIntentResult(intentResult);
    }

    @Given("Raise Intent returns no result")
    public void raiseIntentReturnsNoResult() {
        world.getMessaging().setIntentResult(new TestMessaging.PossibleIntentResult());
    }

    @Given("Raise Intent times out")
    public void raiseIntentTimesOut() {
        TestMessaging.PossibleIntentResult intentResult = new TestMessaging.PossibleIntentResult();
        intentResult.setTimeout(true);
        world.getMessaging().setIntentResult(intentResult);
    }

    @Given("Raise Intent returns an app channel")
    public void raiseIntentReturnsAppChannel() {
        // Would need a Channel implementation
        TestMessaging.PossibleIntentResult intentResult = new TestMessaging.PossibleIntentResult();
        // intentResult.setChannel(...);
        world.getMessaging().setIntentResult(intentResult);
    }

    @Given("Raise Intent returns a user channel")
    public void raiseIntentReturnsUserChannel() {
        TestMessaging.PossibleIntentResult intentResult = new TestMessaging.PossibleIntentResult();
        // intentResult.setChannel(...);
        world.getMessaging().setIntentResult(intentResult);
    }

    @Given("Raise Intent returns a private channel")
    public void raiseIntentReturnsPrivateChannel() {
        TestMessaging.PossibleIntentResult intentResult = new TestMessaging.PossibleIntentResult();
        // intentResult.setChannel(...);
        world.getMessaging().setIntentResult(intentResult);
    }

    @Given("{string} is a intentEvent message with intent {string} and context {string}")
    public void isAIntentEventMessage(String field, String intent, String context) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "intentEvent");

        Map<String, Object> meta = new HashMap<>();
        meta.put("eventUuid", world.getMessaging().createUUID());
        meta.put("timestamp", Instant.now().toString());
        message.put("meta", meta);

        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> originatingApp = new HashMap<>();
        originatingApp.put("appId", "some-app-id");
        originatingApp.put("desktopAgent", "some-desktop-agent");
        payload.put("originatingApp", originatingApp);
        payload.put("context", handleResolve(context, world));
        payload.put("intent", intent);
        payload.put("raiseIntentRequestUuid", "request-id");
        message.put("payload", payload);

        world.set(field, message);
    }

    @Given("{string} pipes intent to {string}")
    public void pipesIntentTo(String intentHandlerName, String field) {
        List<Map<String, Object>> intents = new ArrayList<>();
        world.set(field, intents);
        world.set(intentHandlerName, (BiConsumer<Context, ContextMetadata>) (context, metadata) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("context", context);
            item.put("metadata", metadata);
            intents.add(item);
        });
    }

    @Given("{string} returns a context item")
    public void returnsAContextItem(String intentHandlerName) {
        world.set(intentHandlerName, (Supplier<Context>) () -> {
            Map<Object, Object> id = new HashMap<>();
            id.put("in", "one");
            id.put("out", "two");
            return new Context("fdc3.returned-intent", null, id);
        });
    }

    @Given("{string} returns a channel")
    public void returnsAChannel(String intentHandlerName) {
        world.set(intentHandlerName, (Supplier<Map<String, Object>>) () -> {
            Map<String, Object> channel = new HashMap<>();
            channel.put("type", "private");
            channel.put("id", "some-channel-id");
            Map<String, Object> displayMetadata = new HashMap<>();
            displayMetadata.put("color", "ochre");
            displayMetadata.put("name", "Some Channel");
            channel.put("displayMetadata", displayMetadata);
            return channel;
        });
    }

    @Given("{string} returns a void promise")
    public void returnsAVoidPromise(String intentHandlerName) {
        world.set(intentHandlerName, (Supplier<Object>) () -> null);
    }

    private AppIdentifier createAppIdentifier(String appId, String instanceId) {
        return new AppIdentifier() {
            @Override
            public String getAppId() {
                return appId;
            }

            @Override
            public Optional<String> getInstanceId() {
                return Optional.ofNullable(instanceId);
            }
        };
    }

    private TestMessaging.IntentDetail createIntentDetail(AppIdentifier app, String intent, String context, String resultType) {
        TestMessaging.IntentDetail detail = new TestMessaging.IntentDetail();
        detail.setApp(app);
        detail.setIntent(intent);
        detail.setContext(context);
        detail.setResultType(resultType);
        return detail;
    }
}

