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

package org.finos.fdc3.proxy.world;

import org.finos.fdc3.api.types.EventHandler;
import org.finos.fdc3.proxy.support.TestMessaging;

import io.github.robmoffat.world.PropsWorld;

/**
 * Custom Cucumber World for agent-proxy tests.
 * Extends PropsWorld and adds messaging support.
 * 
 * Bean configuration in TestSpringConfig ensures the same instance is shared
 * across all step definition classes within a scenario.
 */
public class CustomWorld extends PropsWorld {

    private TestMessaging messaging;

    /**
     * standard-cucumber-steps stores invocation counters as {@link Runnable}, but FDC3
     * event listener APIs require {@link EventHandler}. Adapt on lookup so feature
     * files can keep the canonical step wording unchanged.
     */
    @Override
    public Object get(String key) {
        return adaptInvocationCounter(super.get(key));
    }

    @Override
    public Object get(Object key) {
        if (key instanceof String) {
            return get((String) key);
        }
        return adaptInvocationCounter(super.get(key));
    }

    private static Object adaptInvocationCounter(Object value) {
        if (value instanceof Runnable && !(value instanceof EventHandler)) {
            Runnable runnable = (Runnable) value;
            return (EventHandler) event -> runnable.run();
        }
        return value;
    }

    public TestMessaging getMessaging() {
        return messaging;
    }

    public void setMessaging(TestMessaging messaging) {
        this.messaging = messaging;
    }

    public boolean hasMessaging() {
        return messaging != null;
    }
}

