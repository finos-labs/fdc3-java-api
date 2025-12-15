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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.finos.fdc3.api.context.Context;
import org.finos.fdc3.proxy.Connectable;
import org.finos.fdc3.proxy.DesktopAgentProxy;
import org.finos.fdc3.proxy.apps.DefaultAppSupport;
import org.finos.fdc3.proxy.channels.DefaultChannelSupport;
import org.finos.fdc3.proxy.heartbeat.DefaultHeartbeatSupport;
import org.finos.fdc3.proxy.intents.DefaultIntentSupport;
import org.finos.fdc3.proxy.support.SimpleChannelSelector;
import org.finos.fdc3.proxy.support.SimpleIntentResolver;
import org.finos.fdc3.proxy.support.TestMessaging;
import org.finos.fdc3.proxy.world.CustomWorld;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

/**
 * Agent step definitions for agent-proxy tests.
 */
public class AgentSteps {

    private final CustomWorld world;

    public AgentSteps(CustomWorld world) {
        this.world = world;
    }

    @Given("A Desktop Agent in {string}")
    public void aDesktopAgentIn(String field) throws Exception {
        if (!world.hasMessaging()) {
            @SuppressWarnings("unchecked")
            Map<String, List<Context>> channelState = (Map<String, List<Context>>) world.get(ChannelSteps.CHANNEL_STATE);
            world.setMessaging(new TestMessaging(channelState != null ? channelState : new HashMap<>()));
        }

        TestMessaging messaging = world.getMessaging();
        
        // Using short timeouts to avoid extending tests unnecessarily
        DefaultChannelSupport cs = new DefaultChannelSupport(messaging, new SimpleChannelSelector(world), 1500);
        DefaultHeartbeatSupport hs = new DefaultHeartbeatSupport(messaging, 30000);
        DefaultIntentSupport is = new DefaultIntentSupport(messaging, new SimpleIntentResolver(world), 1500, 3000);
        DefaultAppSupport as = new DefaultAppSupport(messaging, 1500, 3000);
        
        List<Connectable> connectables = new ArrayList<>();
        connectables.add(hs);
        
        DesktopAgentProxy da = new DesktopAgentProxy(hs, cs, is, as, connectables);
        da.connect().toCompletableFuture().get();
        
        world.set(field, da);
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
