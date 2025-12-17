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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.cucumber.java.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cucumber World class that holds test state in a props map.
 * This is equivalent to the TypeScript PropsWorld class.
 */
public class PropsWorld implements Map<String, Object>{

    private static final Logger logger = LoggerFactory.getLogger(PropsWorld.class);

    private final Map<String, Object> props = new HashMap<>();
    private Scenario scenario;

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
     * Set the Cucumber scenario for logging.
     * This should be called from a @Before hook.
     * 
     * @param scenario the current Cucumber scenario
     */
    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    /**
     * Get the current scenario.
     * 
     * @return the current Cucumber scenario
     */
    public Scenario getScenario() {
        return scenario;
    }

    /**
     * Log a message to the Cucumber report.
     * This is equivalent to this.log() in TypeScript Cucumber World.
     * 
     * @param message the message to log
     */
    public void log(String message) {
        // Log to SLF4J for console output
        logger.info(message);
        
        // Log to Cucumber report if scenario is available
        if (scenario != null) {
            scenario.log(message);
        }
    }

    /**
     * Attach content to the Cucumber report.
     * 
     * @param data the data to attach
     * @param mediaType the MIME type of the data
     * @param name optional name for the attachment
     */
    public void attach(byte[] data, String mediaType, String name) {
        if (scenario != null) {
            scenario.attach(data, mediaType, name);
        }
    }

    /**
     * Attach text content to the Cucumber report.
     * 
     * @param data the text to attach
     * @param mediaType the MIME type (e.g., "text/plain", "application/json")
     */
    public void attach(String data, String mediaType) {
        if (scenario != null) {
            scenario.attach(data, mediaType, null);
        }
    }

	@Override
	public int size() {
		return props.size();
	}

	@Override
	public boolean isEmpty() {
		return props.size() == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		return props.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return props.containsValue(value);

	}
	
	@Override
	public Object put(String key, Object value) {
		return props.put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return props.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		props.putAll(m);
	}

	@Override
	public void clear() {
		props.clear();
	}

	@Override
	public Set<String> keySet() {
		return props.keySet();
	}

	@Override
	public Collection<Object> values() {
		return props.values();
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		return props.entrySet();
	}

	@Override
	public Object get(Object key) {
		return props.get(key);
	}
}
