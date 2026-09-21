/**
 * SPDX-License-Identifier: Apache-2.0
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

package org.finos.fdc3.api.errors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Errors that can be encountered when calling channel-related methods.
 */
public enum ChannelError {

    /**
     * Returned if the specified channel is not found when attempting to join a channel via
     * {@code joinUserChannel}.
     */
    NoChannelFound("NoChannelFound"),

    /**
     * Should be returned when a request to join a user channel, or to retrieve a Channel object
     * via {@code joinUserChannel} or {@code getOrCreateChannel}, is denied.
     */
    AccessDenied("AccessDenied"),

    /**
     * Should be returned when a channel cannot be created or retrieved via
     * {@code getOrCreateChannel}.
     */
    CreationFailed("CreationFailed"),

    /**
     * Returned if a call to {@code broadcast} is made with an invalid context argument. Contexts
     * should be objects with at least a {@code type} field that has a string value.
     */
    MalformedContext("MalformedContext"),

    /** Returned if a timeout occurs before any channel-related API call is resolved. */
    ApiTimeout("ApiTimeout"),

    /** Returned when incorrect arguments are passed to API calls. */
    InvalidArguments("InvalidArguments");

    private final String value;

    ChannelError(String value) {
        this.value = value;
    }

    /** The value carried in DACP messages, which is not always the constant name. */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Maps a value received from a Desktop Agent back to a constant.
     *
     * @param value the value as it appears on the wire
     * @return the matching constant
     * @throws IllegalArgumentException if no constant carries that value
     */
    @JsonCreator
    public static ChannelError fromValue(String value) {
        for (ChannelError candidate : values()) {
            if (candidate.value.equals(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unknown ChannelError value: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
