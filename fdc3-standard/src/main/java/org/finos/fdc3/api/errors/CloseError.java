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
 * Errors that can be encountered when calling the {@code close} method on the DesktopAgent.
 */
public enum CloseError {

    /** Returned if the Desktop Agent cannot close the app's window or frame. */
    ErrorOnClose("ErrorOnClose"),

    /**
     * Returned if a timeout occurs before a call to close is resolved, for any reason other than
     * the app being closed.
     */
    ApiTimeout("ApiTimeout");

    private final String value;

    CloseError(String value) {
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
    public static CloseError fromValue(String value) {
        for (CloseError candidate : values()) {
            if (candidate.value.equals(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unknown CloseError value: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
