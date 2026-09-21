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
 * Errors that can be encountered when calling the {@code open} method on the DesktopAgent.
 */
public enum OpenError {

    /** Returned if the specified application is not found. */
    AppNotFound("AppNotFound"),

    /** Returned if the specified application fails to launch correctly. */
    ErrorOnLaunch("ErrorOnLaunch"),

    /**
     * Returned if the specified application launches but fails to add a context listener in
     * order to receive the context passed to the {@code fdc3.open} call.
     */
    AppTimeout("AppTimeout"),

    /**
     * Returned if the FDC3 desktop agent implementation is not currently able to handle the
     * request.
     */
    ResolverUnavailable("ResolverUnavailable"),

    /**
     * Returned if a call to the {@code open} function is made with an invalid context argument.
     * Contexts should be objects with at least a {@code type} field that has a string value.
     */
    MalformedContext("MalformedContext"),

    /**
     * Returned if the specified Desktop Agent is not found, via a connected Desktop Agent
     * Bridge. Experimental.
     */
    DesktopAgentNotFound("DesktopAgentNotFound"),

    /**
     * Returned if a timeout occurs before a call to open is resolved for any reason other than
     * the app not adding its context listener in time.
     */
    ApiTimeout("ApiTimeout"),

    /** Returned when incorrect arguments are passed to API calls. */
    InvalidArguments("InvalidArguments");

    private final String value;

    OpenError(String value) {
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
    public static OpenError fromValue(String value) {
        for (OpenError candidate : values()) {
            if (candidate.value.equals(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unknown OpenError value: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
