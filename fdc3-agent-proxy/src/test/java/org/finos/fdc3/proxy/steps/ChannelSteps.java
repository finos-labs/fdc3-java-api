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

package org.finos.fdc3.proxy.steps;

import static org.finos.fdc3.testing.support.MatchingUtils.handleResolve;
import static org.finos.fdc3.testing.support.MatchingUtils.matchData;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.api.metadata.ContextMetadata;
import org.finos.fdc3.api.types.ContextHandler;
import org.finos.fdc3.api.types.EventHandler;
import org.finos.fdc3.api.types.FDC3Event;
import org.finos.fdc3.proxy.support.ContextMap;
import org.finos.fdc3.proxy.world.CustomWorld;
import org.finos.fdc3.testing.steps.GenericSteps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Cucumber step definitions for channel-related tests.
 */
public class ChannelSteps {

    public static final String CHANNEL_STATE = "CHANNEL_STATE";

    private final CustomWorld world;

    public ChannelSteps(CustomWorld world) {
        this.world = world;
    }

    @Given("{string} is a {string} context")
    public void isAContext(String field, String type) {
        world.set(field, ContextMap.get(type));
    }

    @Given("{string} is a BroadcastEvent message on channel {string} with context {string}")
    public void isABroadcastEventMessage(String field, String channel, String contextType) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "broadcastEvent");
        message.put("meta", world.getMessaging().createEventMeta());

        Map<String, Object> payload = new HashMap<>();
        payload.put("channelId", handleResolve(channel, world));
        payload.put("context", ContextMap.get(contextType));
        message.put("payload", payload);

        world.set(field, message);
    }

    @Given("{string} is a {string} message on channel {string}")
    public void isAMessageOnChannel(String field, String type, String channel) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.put("meta", world.getMessaging().createEventMeta());

        Map<String, Object> payload = new HashMap<>();
        payload.put("privateChannelId", handleResolve(channel, world));
        message.put("payload", payload);

        world.set(field, message);
    }

    @Given("{string} is a {string} message on channel {string} with listenerType as {string}")
    public void isAMessageOnChannelWithListenerType(String field, String type, String channel, String listenerType) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.put("meta", world.getMessaging().createMeta());

        Map<String, Object> payload = new HashMap<>();
        payload.put("channelId", handleResolve(channel, world));
        payload.put("listenerType", listenerType);
        message.put("payload", payload);

        world.set(field, message);
    }

    @Given("{string} is a channelChangedEvent message on channel {string}")
    public void isAChannelChangedEventMessage(String field, String channel) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "channelChangedEvent");

        Map<String, Object> meta = new HashMap<>();
        meta.put("eventUuid", world.getMessaging().createUUID());
        meta.put("timestamp", java.time.Instant.now().toString());
        message.put("meta", meta);

        Map<String, Object> payload = new HashMap<>();
        payload.put("newChannelId", handleResolve(channel, world));
        message.put("payload", payload);

        world.set(field, message);
    }

    @Given("{string} is a PrivateChannelOnUnsubscribeEvent message on channel {string} with contextType as {string}")
    public void isAPrivateChannelOnUnsubscribeEvent(String field, String channel, String contextType) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "privateChannelOnUnsubscribeEvent");
        message.put("meta", world.getMessaging().createEventMeta());

        Map<String, Object> payload = new HashMap<>();
        payload.put("privateChannelId", handleResolve(channel, world));
        payload.put("contextType", contextType);
        message.put("payload", payload);

        world.set(field, message);
    }

    @Given("{string} is a PrivateChannelOnAddContextListenerEvent message on channel {string} with contextType as {string}")
    public void isAPrivateChannelOnAddContextListenerEvent(String field, String channel, String contextType) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "privateChannelOnAddContextListenerEvent");
        message.put("meta", world.getMessaging().createEventMeta());

        Map<String, Object> payload = new HashMap<>();
        payload.put("privateChannelId", handleResolve(channel, world));
        payload.put("contextType", contextType);
        message.put("payload", payload);

        world.set(field, message);
    }

    @Given("{string} is a PrivateChannelOnDisconnectEvent message on channel {string}")
    public void isAPrivateChannelOnDisconnectEvent(String field, String channel) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "privateChannelOnDisconnectEvent");
        message.put("meta", world.getMessaging().createEventMeta());

        Map<String, Object> payload = new HashMap<>();
        payload.put("privateChannelId", handleResolve(channel, world));
        message.put("payload", payload);

        world.set(field, message);
    }

    @Given("{string} pipes types to {string}")
    public void pipesTypesTo(String typeHandlerName, String field) {
        List<String> types = new ArrayList<>();
        world.set(field, types);
        
        class MyHandler implements ContextHandler, EventHandler {
        		
			@Override
			public void handleContext(Context context, ContextMetadata metadata) {
				types.add(context.getType());
				
			}

			@Override
			public void handleEvent(FDC3Event event) {
				@SuppressWarnings("unchecked")
				Map<String, Object> details = (Map<String, Object>) event.getDetails();
				types.add((String) details.get("contextType"));
			}
		};
        
		MyHandler ch = new MyHandler();
		
        world.set(typeHandlerName, ch);
    }

    @Given("{string} pipes events to {string}")
    public void pipesEventsTo(String typeHandlerName, String field) {
        List<Object> events = new ArrayList<>();
        world.set(field, events);
        
        EventHandler eh = new EventHandler() {
			
			@Override
			public void handleEvent(FDC3Event event) {
				events.add(event.getDetails());
			}
		};
        
        world.set(typeHandlerName, eh);
    }

    @Given("{string} pipes context to {string}")
    public void pipesContextTo(String contextHandlerName, String field) {
        List<Context> contexts = new ArrayList<>();
        world.set(field, contexts);
        
        ContextHandler ch = new ContextHandler() {
			
			@Override
			public void handleContext(Context context, ContextMetadata metadata) {
				contexts.add(context);
				
			}
		};
        
        world.set(contextHandlerName, ch);
    }

    @When("messaging receives {string}")
    public void messagingReceives(String field) {
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) handleResolve(field, world);
        System.out.println("Sending: " + message);
        world.getMessaging().receive(message, System.out::println);
    }

    @Then("messaging will have posts")
    public void messagingWillHavePosts(DataTable dt) {
        int matching = dt.height() - 1; // exclude header
        List<Map<String, Object>> toUse = world.getMessaging().getAllPosts();
        if (toUse.size() > matching) {
            toUse = toUse.subList(toUse.size() - matching, toUse.size());
        }
        matchData(world, toUse, dt);
    }

    @Given("channel {string} has context {string}")
    public void channelHasContext(String channel, String context) {
        Context ctxObject = (Context) handleResolve(context, world);
        @SuppressWarnings("unchecked")
        Map<String, List<Context>> state = (Map<String, List<Context>>) world.get(CHANNEL_STATE);
        if (state == null) {
            state = new HashMap<>();
            world.set(CHANNEL_STATE, state);
        }

        List<Context> cs = state.computeIfAbsent(channel, k -> new ArrayList<>());
        cs.add(ctxObject);
    }

    @Given("User Channels one, two and three")
    public void userChannelsOneTwoAndThree() {
        Map<String, List<Context>> state = new HashMap<>();
        state.put("one", new ArrayList<>());
        state.put("two", new ArrayList<>());
        state.put("three", new ArrayList<>());
        world.set(CHANNEL_STATE, state);
    }

    @When("I destructure methods {string}, {string} from {string}")
    public void iDestructureMethods(String method1, String method2, String objectField) {
        Object object = handleResolve(objectField, world);
        // Store references to methods for later invocation
        world.set("destructured_" + method1, new DestructuredMethod(object, method1));
        world.set("destructured_" + method2, new DestructuredMethod(object, method2));
    }

    @When("I destructure method {string} from {string}")
    public void iDestructureMethod(String methodName, String objectField) {
        Object object = handleResolve(objectField, world);
        world.set("destructured_" + methodName, new DestructuredMethod(object, methodName));
    }

    @When("I call destructured {string}")
    public void iCallDestructured(String methodName) {
        try {
            DestructuredMethod dm = (DestructuredMethod) world.get("destructured_" + methodName);
            Object result = dm.invoke();
            world.set("result", result);
        } catch (Exception e) {
            world.set("error", e);
            world.set("result", null);
        }
    }

    @When("I call destructured {string} with parameter {string}")
    public void iCallDestructuredWithParameter(String methodName, String param) {
        try {
            DestructuredMethod dm = (DestructuredMethod) world.get("destructured_" + methodName);
            Object resolvedParam = handleResolve(param, world);
            Object result = dm.invoke(resolvedParam);
            world.set("result", result);
        } catch (Exception e) {
            world.set("error", e);
            world.set("result", null);
        }
    }

    @When("I call destructured {string} with parameters {string} and {string}")
    public void iCallDestructuredWithTwoParameters(String methodName, String param1, String param2) {
        try {
            DestructuredMethod dm = (DestructuredMethod) world.get("destructured_" + methodName);
            Object resolvedParam1 = handleResolve(param1, world);
            Object resolvedParam2 = handleResolve(param2, world);
            Object result = dm.invoke(resolvedParam1, resolvedParam2);
            world.set("result", result);
        } catch (Exception e) {
            world.set("error", e);
            world.set("result", null);
        }
    }

    @When("I call destructured {string} with parameters {string} and {string} and {string}")
    public void iCallDestructuredWithThreeParameters(String methodName, String param1, String param2, String param3) {
        try {
            DestructuredMethod dm = (DestructuredMethod) world.get("destructured_" + methodName);
            Object resolvedParam1 = handleResolve(param1, world);
            Object resolvedParam2 = handleResolve(param2, world);
            Object resolvedParam3 = handleResolve(param3, world);
            Object result = dm.invoke(resolvedParam1, resolvedParam2, resolvedParam3);
            world.set("result", result);
        } catch (Exception e) {
            world.set("error", e);
            world.set("result", null);
        }
    }

    /**
     * Helper class to hold a destructured method reference.
     */
    public static class DestructuredMethod {
        private final Object target;
        private final String methodName;

        public DestructuredMethod(Object target, String methodName) {
            this.target = target;
            this.methodName = methodName;
        }

        public Object invoke(Object... args) throws Exception {
            java.lang.reflect.Method method = GenericSteps.findMethod(target.getClass(), methodName, args);
            if (method == null) {
                throw new NoSuchMethodException("Method not found: " + methodName);
            }

            method.setAccessible(true);
            Object result = method.invoke(target, args);

            // Handle CompletionStage/CompletableFuture
            if (result instanceof java.util.concurrent.CompletionStage) {
                return ((java.util.concurrent.CompletionStage<?>) result).toCompletableFuture().get();
            }
            return result;
        }

    }
}

