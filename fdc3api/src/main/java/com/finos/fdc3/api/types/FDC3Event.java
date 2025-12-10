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

package com.finos.fdc3.api.types;

/**
 * Type representing the event object passed to event handlers subscribed to
 * FDC3 events via the {@code addEventListener} method.
 * 
 * Events will always include both {@code type} and {@code details} properties,
 * which describe the type of event and any additional details, respectively.
 *
 * @param <T> The type of the details object for this event
 */
public class FDC3Event<T> {

    private final String type;
    private final T details;

    public FDC3Event(String type, T details) {
        this.type = type;
        this.details = details;
    }

    /**
     * Returns the type of the event.
     * 
     * @return the event type string (e.g., "userChannelChanged")
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the details object containing additional information about the event.
     * 
     * @return the event details
     */
    public T getDetails() {
        return details;
    }
}

