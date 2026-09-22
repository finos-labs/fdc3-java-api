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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.finos.fdc3.api.metadata.AntiReplayClaims;
import org.finos.fdc3.api.metadata.AppProvidableContextMetadata;
import org.finos.fdc3.api.metadata.ContextMetadata;
import org.finos.fdc3.api.metadata.DetachedSignature;
import org.finos.fdc3.api.types.AppIdentifier;

/**
 * Maps between DACP wire metadata maps and {@link ContextMetadata}.
 */
public final class ContextMetadataMapper {

    private ContextMetadataMapper() {
    }

    /**
     * Outbound metadata for DACP request payloads when the app supplied metadata.
     * Callers must omit the field entirely when the app did not supply any
     * (rather than sending an empty object).
     * <p>
     * Emits only {@code traceId}, {@code signature}, {@code antiReplay} and {@code custom}: the
     * {@code AppProvidableContextMetadata} schema declares {@code additionalProperties: false}
     * over exactly those fields. Receive-side verification results are not reachable through
     * {@link AppProvidableContextMetadata}, so there is nothing further to drop here.
     */
    public static Map<String, Object> toWire(AppProvidableContextMetadata metadata) {
        return toWire(metadata, false, null);
    }

    /**
     * Outbound metadata for intent raise requests when the app supplied metadata.
     * Prefer omitting the field when metadata is null (Desktop Agent owns traceId generation).
     *
     * @deprecated Prefer {@link #toWire(AppProvidableContextMetadata)} and omit when null.
     */
    @Deprecated
    public static Map<String, Object> toWireForIntentRequest(
            AppProvidableContextMetadata metadata,
            Supplier<String> traceIdSupplier) {
        return toWire(metadata, true, traceIdSupplier);
    }

    private static Map<String, Object> toWire(
            AppProvidableContextMetadata metadata,
            boolean ensureTraceId,
            Supplier<String> traceIdSupplier) {
        if (metadata == null) {
            if (ensureTraceId && traceIdSupplier != null) {
                Map<String, Object> wire = new LinkedHashMap<>();
                wire.put("traceId", traceIdSupplier.get());
                return wire;
            }
            return new LinkedHashMap<>();
        }
        if (!(metadata instanceof ContextMetadata)) {
            throw new IllegalArgumentException("metadata must be ContextMetadata");
        }
        ContextMetadata cm = (ContextMetadata) metadata;
        Map<String, Object> wire = new LinkedHashMap<>();

        String traceId = cm.getTraceId();
        if (traceId == null && ensureTraceId && traceIdSupplier != null) {
            traceId = traceIdSupplier.get();
        }
        if (traceId != null) {
            wire.put("traceId", traceId);
        }

        DetachedSignature signature = cm.getSignature();
        if (signature != null) {
            wire.put("signature", signatureToMap(signature));
        }

        AntiReplayClaims antiReplay = cm.getAntiReplay();
        if (antiReplay != null) {
            wire.put("antiReplay", antiReplayToMap(antiReplay));
        }

        Map<String, Object> custom = cm.getCustom();
        if (custom != null) {
            wire.put("custom", new LinkedHashMap<>(custom));
        }

        return wire;
    }

    /**
     * What to record when a received message carries no {@code traceId}.
     * <p>
     * The {@code ContextMetadata} schema makes {@code traceId} required, so something has to be
     * supplied. Which fallback is correct depends on the receive path, and each one mirrors the
     * reference implementation.
     */
    public enum MissingTraceId {

        /**
         * Leave it absent, as {@code DefaultContextListener} does.
         */
        LEAVE_ABSENT,

        /**
         * Record an empty string, as {@code DefaultChannel.getCurrentContext} and intent result
         * metadata do. This satisfies the required field while still saying plainly that no
         * trace id was received.
         */
        EMPTY,

        /**
         * Generate one, as {@code DefaultIntentListener} does. Only correct where the app is
         * expected to continue a trace it cannot otherwise identify.
         */
        GENERATE
    }

    public static ContextMetadata fromWire(
            Map<String, Object> payloadMetadata, Object messageTimestamp, MissingTraceId missingTraceId) {
        return fromWire(payloadMetadata, messageTimestamp, null, missingTraceId);
    }

    /**
     * Builds listener metadata from wire payload fields and optional message {@code meta} (e.g. event source).
     * <p>
     * A {@code traceId} correlates a message across applications, so one is never invented here
     * beyond what {@code missingTraceId} asks for. Generating a fresh identifier on every receive
     * path, as this once did, produced trace ids that correlated with nothing and hid the
     * difference between a sender that supplied one and a sender that did not.
     */
    @SuppressWarnings("unchecked")
    public static ContextMetadata fromWire(
            Map<String, Object> payloadMetadata,
            Object messageTimestamp,
            Map<String, Object> messageMeta,
            MissingTraceId missingTraceId) {
        ContextMetadata metadata = ContextMetadata.fromMap(payloadMetadata);
        if (metadata == null) {
            metadata = ContextMetadata.appProvidable();
        }
        if (metadata.getTimestamp() == null && messageTimestamp != null) {
            if (messageTimestamp instanceof Instant) {
                metadata.setTimestamp((Instant) messageTimestamp);
            } else {
                metadata.setTimestamp(Instant.parse(String.valueOf(messageTimestamp)));
            }
        }
        if (metadata.getTraceId() == null || metadata.getTraceId().isEmpty()) {
            switch (missingTraceId) {
                case GENERATE:
                    metadata.setTraceId(UUID.randomUUID().toString());
                    break;
                case EMPTY:
                    metadata.setTraceId("");
                    break;
                case LEAVE_ABSENT:
                default:
                    break;
            }
        }
        applyMetaSourceIfAbsent(metadata, messageMeta);
        return metadata;
    }

    @SuppressWarnings("unchecked")
    private static void applyMetaSourceIfAbsent(ContextMetadata metadata, Map<String, Object> messageMeta) {
        if (metadata.getSource() != null || messageMeta == null) {
            return;
        }
        Object source = messageMeta.get("source");
        if (source instanceof AppIdentifier) {
            metadata.setSource((AppIdentifier) source);
        } else if (source instanceof Map) {
            metadata.setSource(AppIdentifier.fromMap((Map<String, Object>) source));
        }
    }

    private static Map<String, Object> signatureToMap(DetachedSignature signature) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (signature.getProtectedHeader() != null) {
            map.put("protected", signature.getProtectedHeader());
        }
        if (signature.getSignature() != null) {
            map.put("signature", signature.getSignature());
        }
        return map;
    }

    private static Map<String, Object> antiReplayToMap(AntiReplayClaims antiReplay) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("iat", antiReplay.getIat());
        map.put("exp", antiReplay.getExp());
        map.put("jti", antiReplay.getJti());
        return map;
    }
}
