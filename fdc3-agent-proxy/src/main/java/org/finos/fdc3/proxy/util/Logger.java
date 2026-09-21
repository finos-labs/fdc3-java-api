/**
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

package org.finos.fdc3.proxy.util;

import org.slf4j.LoggerFactory;

/**
 * Simple logging utility for the FDC3 Agent Proxy.
 * <p>
 * Log levels are configured through the SLF4J binding (e.g., Logback, Log4j2).
 */
public final class Logger {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger("fdc3.proxy");

    /**
     * Separate logger for full DACP message bodies, which carry application context and,
     * during the WSCP handshake, the connection secret. Kept apart from {@link #log} so that
     * enabling DEBUG on the proxy does not disclose message contents.
     */
    private static final org.slf4j.Logger payloadLog = LoggerFactory.getLogger("fdc3.proxy.payload");

    private Logger() {
        // Utility class
    }

    /**
     * Whether full message bodies would be recorded. Callers should check this before doing
     * the work of redacting and serialising a payload.
     *
     * @return true if the payload trace logger is enabled
     */
    public static boolean isPayloadEnabled() {
        return payloadLog.isTraceEnabled();
    }

    /**
     * Logs a full message body to the payload trace logger. Callers are responsible for
     * redacting sensitive fields first; see
     * {@link MessageLogging#redact(Object)}.
     */
    public static void payload(String message, Object... args) {
        payloadLog.trace(message, args);
    }

    public static void debug(String message) {
        log.debug(message);
    }

    public static void debug(String message, Object... args) {
        log.debug(message, args);
    }

    public static void info(String message) {
        log.info(message);
    }

    public static void info(String message, Object... args) {
        log.info(message, args);
    }

    public static void warn(String message) {
        log.warn(message);
    }

    public static void warn(String message, Object... args) {
        log.warn(message, args);
    }

    public static void error(String message) {
        log.error(message);
    }

    public static void error(String message, Object... args) {
        log.error(message, args);
    }

    public static void error(String message, Throwable throwable) {
        log.error(message, throwable);
    }
}
