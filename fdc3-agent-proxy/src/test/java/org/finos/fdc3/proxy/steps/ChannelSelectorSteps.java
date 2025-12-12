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
import org.finos.fdc3.proxy.support.TestChannelSelector;
import org.finos.fdc3.proxy.support.TestMessaging;
import org.finos.fdc3.proxy.world.CustomWorld;
import org.finos.fdc3.testing.agent.SimpleIntentResolver;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

/**
 * Cucumber step definitions for channel selector tests.
 */
public class ChannelSelectorSteps {

    private final CustomWorld world;

    public ChannelSelectorSteps(CustomWorld world) {
        this.world = world;
    }

    @Given("A Channel Selector in {string} and a Desktop Agent in {string}")
    public void aChannelSelectorAndDesktopAgent(String selectorField, String daField) {
        if (!world.hasMessaging()) {
            @SuppressWarnings("unchecked")
            Map<String, List<Context>> channelState = (Map<String, List<Context>>) world.get(ChannelSteps.CHANNEL_STATE);
            world.setMessaging(new TestMessaging(channelState != null ? channelState : new HashMap<>()));
        }

        TestChannelSelector ts = new TestChannelSelector();
        world.set(selectorField, ts);

        // TODO: Create DefaultChannelSupport, DefaultHeartbeatSupport, etc.
        // For now, we just set up the basic structure

        // Store the selector and desktop agent references
        world.set(daField, new Object()); // Placeholder - replace with actual DesktopAgentProxy
        world.set("result", null);
    }

    @When("The first channel is selected via the channel selector in {string}")
    public void theFirstChannelIsSelected(String selectorField) {
        TestChannelSelector selector = (TestChannelSelector) world.get(selectorField);
        selector.selectFirstChannel();
    }

    @When("The second channel is selected via the channel selector in {string}")
    public void theSecondChannelIsSelected(String selectorField) {
        TestChannelSelector selector = (TestChannelSelector) world.get(selectorField);
        selector.selectSecondChannel();
    }

    @When("The channel is deselected via the channel selector in {string}")
    public void theChannelIsDeselected(String selectorField) {
        TestChannelSelector selector = (TestChannelSelector) world.get(selectorField);
        selector.deselectChannel();
    }
}

