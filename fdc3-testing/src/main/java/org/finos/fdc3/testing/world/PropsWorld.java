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

package org.finos.fdc3.testing.world;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cucumber World class that holds test state in a props map.
 * This is equivalent to the TypeScript PropsWorld class.
 */
public class PropsWorld {

    private static final Logger logger = LoggerFactory.getLogger(PropsWorld.class);

    private final Map<String, Object> props = new HashMap<>();

    /**
     * Get the props map containing all test state.
     * 
     * @return the props map
     */
    public Map<String, Object> getProps() {
        return props;
    }

    /**
     * Get a property value by key.
     * 
     * @param key the property key
     * @return the property value, or null if not found
     */
    public Object get(String key) {
        return props.get(key);
    }

    /**
     * Set a property value.
     * 
     * @param key   the property key
     * @param value the property value
     */
    public void set(String key, Object value) {
        props.put(key, value);
    }

    /**
     * Check if a property exists.
     * 
     * @param key the property key
     * @return true if the property exists
     */
    public boolean has(String key) {
        return props.containsKey(key);
    }

    /**
     * Log a message (equivalent to cw.log in TypeScript).
     * 
     * @param message the message to log
     */
    public void log(String message) {
        logger.info(message);
    }
}

