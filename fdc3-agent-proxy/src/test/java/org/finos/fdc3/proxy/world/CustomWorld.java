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

package org.finos.fdc3.proxy.world;

import org.finos.fdc3.proxy.support.TestMessaging;
import org.finos.fdc3.testing.world.PropsWorld;

/**
 * Custom Cucumber World for agent-proxy tests.
 * Extends PropsWorld and adds messaging support.
 */
public class CustomWorld extends PropsWorld {

    private TestMessaging messaging;

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

