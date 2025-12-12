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

package org.finos.fdc3.testing;

import org.finos.fdc3.testing.agent.ChannelSelector;
import org.finos.fdc3.testing.agent.IntentResolver;
import org.finos.fdc3.testing.agent.SimpleChannelSelector;
import org.finos.fdc3.testing.agent.SimpleIntentResolver;
import org.finos.fdc3.testing.steps.GenericSteps;
import org.finos.fdc3.testing.support.MatchingUtils;
import org.finos.fdc3.testing.world.PropsWorld;

/**
 * Main entry point for the FDC3 Testing framework.
 * <p>
 * This class provides convenient access to all testing components and constants.
 * <p>
 * Usage example:
 * <pre>
 * // Create a test world
 * PropsWorld world = new PropsWorld();
 *
 * // Create step definitions (automatically registered with Cucumber)
 * GenericSteps steps = new GenericSteps(world);
 *
 * // Use matching utilities
 * Object value = MatchingUtils.handleResolve("{result.field}", world);
 *
 * // Create test agents
 * IntentResolver resolver = new SimpleIntentResolver(world);
 * ChannelSelector selector = new SimpleChannelSelector(world);
 * </pre>
 */
public final class FDC3Testing {

    /**
     * Constant for channel state key in PropsWorld.
     */
    public static final String CHANNEL_STATE = SimpleChannelSelector.CHANNEL_STATE;

    private FDC3Testing() {
        // Utility class - prevent instantiation
    }

    /**
     * Create a new PropsWorld instance for test state.
     *
     * @return a new PropsWorld
     */
    public static PropsWorld createWorld() {
        return new PropsWorld();
    }

    /**
     * Create a new SimpleIntentResolver for testing.
     *
     * @param world the PropsWorld to use
     * @return a new SimpleIntentResolver
     */
    public static IntentResolver createIntentResolver(PropsWorld world) {
        return new SimpleIntentResolver(world);
    }

    /**
     * Create a new SimpleChannelSelector for testing.
     *
     * @param world the PropsWorld to use
     * @return a new SimpleChannelSelector
     */
    public static ChannelSelector createChannelSelector(PropsWorld world) {
        return new SimpleChannelSelector(world);
    }

    /**
     * Get the MatchingUtils class for static utility access.
     *
     * @return the MatchingUtils class
     */
    public static Class<MatchingUtils> getMatchingUtils() {
        return MatchingUtils.class;
    }

    /**
     * Get the GenericSteps class for step definition registration.
     *
     * @return the GenericSteps class
     */
    public static Class<GenericSteps> getGenericSteps() {
        return GenericSteps.class;
    }
}

