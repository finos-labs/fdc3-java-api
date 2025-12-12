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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.proxy.support.TestMessaging;
import org.finos.fdc3.proxy.world.CustomWorld;
import org.finos.fdc3.testing.agent.SimpleChannelSelector;
import org.finos.fdc3.testing.agent.SimpleIntentResolver;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

/**
 * Generic Cucumber step definitions for agent-proxy tests.
 */
public class GenericSteps {

    private final CustomWorld world;

    public GenericSteps(CustomWorld world) {
        this.world = world;
    }

    @Given("A Desktop Agent in {string}")
    public void aDesktopAgentIn(String field) {
        if (!world.hasMessaging()) {
            @SuppressWarnings("unchecked")
            Map<String, List<Context>> channelState = (Map<String, List<Context>>) world.get(ChannelSteps.CHANNEL_STATE);
            world.setMessaging(new TestMessaging(channelState != null ? channelState : new HashMap<>()));
        }

        // TODO: Create actual DesktopAgentProxy with:
        // - DefaultChannelSupport
        // - DefaultHeartbeatSupport
        // - DefaultIntentSupport
        // - DefaultAppSupport

        // For now, store a placeholder
        world.set(field, new Object()); // Replace with actual DesktopAgentProxy
        world.set("result", null);
    }

    @When("messaging receives a heartbeat event")
    public void messagingReceivesHeartbeatEvent() {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "heartbeatEvent");
        message.put("meta", world.getMessaging().createEventMeta());

        Map<String, Object> payload = new HashMap<>();
        payload.put("timestamp", java.time.Instant.now().toString());
        message.put("payload", payload);

        world.getMessaging().receive(message, null);
    }

}

