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
     * Outbound metadata for DACP request payloads (reference TS: {@code metadata ?? {}}).
     */
    public static Map<String, Object> toWire(AppProvidableContextMetadata metadata) {
        return toWire(metadata, false, null);
    }

    /**
     * Outbound metadata for intent raise requests (reference TS: always includes {@code traceId}).
     */
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

    public static ContextMetadata fromWire(Map<String, Object> payloadMetadata, Object messageTimestamp) {
        return fromWire(payloadMetadata, messageTimestamp, null);
    }

    /**
     * Builds listener metadata from wire payload fields and optional message {@code meta} (e.g. event source).
     */
    @SuppressWarnings("unchecked")
    public static ContextMetadata fromWire(
            Map<String, Object> payloadMetadata,
            Object messageTimestamp,
            Map<String, Object> messageMeta) {
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
            metadata.setTraceId(UUID.randomUUID().toString());
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
