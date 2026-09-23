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
 * Errors that can be encountered when retrieving an intent result.
 */
public enum ResultError {

    /**
     * Returned if the intent handler exited without returning a valid result: a Context, a
     * Channel, or nothing at all.
     */
    NoResultReturned("NoResultReturned"),

    /**
     * Returned if the intent handler function processing the raised intent throws an error or
     * rejects the promise it returned.
     */
    IntentHandlerRejected("IntentHandlerRejected"),

    /** Returned if a timeout occurs before the {@code getResult()} call is resolved. */
    ApiTimeout("ApiTimeout");

    private final String value;

    ResultError(String value) {
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
    public static ResultError fromValue(String value) {
        for (ResultError candidate : values()) {
            if (candidate.value.equals(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unknown ResultError value: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
