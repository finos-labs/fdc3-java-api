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
 * Errors that can be encountered when calling {@code findIntent}, {@code findIntentsByContext},
 * {@code raiseIntent} or {@code raiseIntentForContext}.
 */
public enum ResolveError {

    /**
     * Should be returned if no apps are available that can resolve the intent and context
     * combination.
     */
    NoAppsFound("NoAppsFound"),

    /**
     * Returned if the FDC3 desktop agent implementation is not currently able to handle the
     * request.
     */
    ResolverUnavailable("ResolverUnavailable"),

    /**
     * Returned if the user cancelled the resolution request, for example by closing or
     * cancelling a resolver UI. Note that the wire value differs from the constant name.
     */
    UserCancelled("UserCancelledResolution"),

    /**
     * Should be returned if a timeout cancels an intent resolution that required user
     * interaction. Use {@link #ResolverUnavailable} where a resolver UI itself fails.
     */
    ResolverTimeout("ResolverTimeout"),

    /**
     * Returned if a specified target application is not available or a new instance of it
     * cannot be opened.
     */
    TargetAppUnavailable("TargetAppUnavailable"),

    /**
     * Returned if a specified target application instance is not available, for example because
     * it has been closed.
     */
    TargetInstanceUnavailable("TargetInstanceUnavailable"),

    /**
     * Returned if the intent and context could not be delivered to the selected application or
     * instance, for example because it did not add an intent handler within a timeout.
     */
    IntentDeliveryFailed("IntentDeliveryFailed"),

    /**
     * Returned if a call to one of the {@code raiseIntent} functions is made with an invalid
     * context argument. Contexts should be objects with at least a {@code type} field that has
     * a string value.
     */
    MalformedContext("MalformedContext"),

    /**
     * Returned if {@code fdc3.addIntentListener} is called for an intent the application has
     * already added a listener for and has not subsequently removed.
     */
    IntentListenerConflict("IntentListenerConflict"),

    /**
     * Returned if the specified Desktop Agent is not found, via a connected Desktop Agent
     * Bridge. Experimental.
     */
    DesktopAgentNotFound("DesktopAgentNotFound"),

    /**
     * Returned if a timeout occurs before the API call is resolved, for any reason other than
     * the resolver timing out (use {@link #ResolverTimeout}) or an app launched by a
     * {@code raiseIntent} function not adding its intent listener in time (use
     * {@link #IntentDeliveryFailed}).
     */
    ApiTimeout("ApiTimeout"),

    /** Returned when incorrect arguments are passed to API calls. */
    InvalidArguments("InvalidArguments");

    private final String value;

    ResolveError(String value) {
        this.value = value;
    }

    /** The value carried in DACP messages, which is not always the constant name. */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Maps a value received from a Desktop Agent back to a constant. This is the only correct
     * way to interpret a received error string, since {@link #UserCancelled} is transmitted as
     * {@code UserCancelledResolution}.
     *
     * @param value the value as it appears on the wire
     * @return the matching constant
     * @throws IllegalArgumentException if no constant carries that value
     */
    @JsonCreator
    public static ResolveError fromValue(String value) {
        for (ResolveError candidate : values()) {
            if (candidate.value.equals(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unknown ResolveError value: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
