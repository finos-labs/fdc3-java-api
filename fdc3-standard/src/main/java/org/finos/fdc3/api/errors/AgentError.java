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
 * Errors that can be encountered when trying to connect to a Desktop Agent.
 */
public enum AgentError {

    /**
     * Returned if no Desktop Agent was found by any means available, or if an agent previously
     * connected to is not contactable on a subsequent connection attempt.
     */
    AgentNotFound("AgentNotFound"),

    /**
     * Returned if validation of the app identity by the Desktop Agent failed, or the app is not
     * being allowed to connect for another reason.
     */
    AccessDenied("AccessDenied"),

    /**
     * Returned if an error or exception occurs while trying to set up communication with a
     * Desktop Agent.
     */
    ErrorOnConnect("ErrorOnConnect"),

    /**
     * Returned if the failover function is not a function, or it did not resolve to one of the
     * allowed types.
     */
    InvalidFailover("InvalidFailover"),

    /**
     * Returned if an API call rejects after a timeout, where the call is not aligned to another
     * error enumeration.
     */
    ApiTimeout("ApiTimeout");

    private final String value;

    AgentError(String value) {
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
    public static AgentError fromValue(String value) {
        for (AgentError candidate : values()) {
            if (candidate.value.equals(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unknown AgentError value: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
