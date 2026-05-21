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

import org.finos.fdc3.api.metadata.AppProvidableContextMetadata;
import org.finos.fdc3.api.metadata.ContextMetadata;

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
        return metadata == null ? new LinkedHashMap<>() : ((ContextMetadata) metadata).toMap();
    }

    public static ContextMetadata fromWire(Map<String, Object> payloadMetadata, Object messageTimestamp) {
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
        return metadata;
    }
}
