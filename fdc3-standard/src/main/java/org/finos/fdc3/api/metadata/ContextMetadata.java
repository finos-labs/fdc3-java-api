/*
 * Copyright FINOS and Contributors to the FDC3 project
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
package org.finos.fdc3.api.metadata;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.finos.fdc3.api.types.AppIdentifier;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Map-backed metadata for context and intent messages.
 * <p>
 * Implements {@link AppProvidableContextMetadata} for outbound app-provided fields and
 * {@link DesktopAgentProvidableContextMetadata} for optional Desktop Agent fields.
 * When received via listeners, {@link #getSource()} and {@link #getTimestamp()} are
 * typically populated by the Desktop Agent.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContextMetadata implements AppProvidableContextMetadata, DesktopAgentProvidableContextMetadata {

    private final Map<String, Object> data;

    public ContextMetadata() {
        this.data = new LinkedHashMap<>();
    }

    public ContextMetadata(Map<String, Object> initial) {
        this.data = initial != null ? new LinkedHashMap<>(initial) : new LinkedHashMap<>();
    }

    /** Returns a new empty instance suitable for outbound app-provided metadata. */
    public static ContextMetadata appProvidable() {
        return new ContextMetadata();
    }

    /** Returns an unmodifiable view of the backing map. */
    @JsonIgnore
    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(data);
    }

    /** Returns a mutable copy of the backing map (for wire serialization). */
    @JsonIgnore
    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(data);
    }

    @JsonAnyGetter
    public Map<String, Object> jsonProperties() {
        return data;
    }

    @JsonAnySetter
    public void jsonProperty(String key, Object value) {
        data.put(key, value);
    }

    @Override
    @JsonProperty("traceId")
    public String getTraceId() {
        return (String) data.get("traceId");
    }

    @Override
    public void setTraceId(String traceId) {
        putOrRemove("traceId", traceId);
    }

    @Override
    @JsonProperty("signature")
    public DetachedSignature getSignature() {
        return getTyped("signature", DetachedSignature.class);
    }

    @Override
    public void setSignature(DetachedSignature signature) {
        putOrRemove("signature", signature);
    }

    @Override
    @JsonProperty("antiReplay")
    public AntiReplayClaims getAntiReplay() {
        return getTyped("antiReplay", AntiReplayClaims.class);
    }

    @Override
    public void setAntiReplay(AntiReplayClaims antiReplay) {
        putOrRemove("antiReplay", antiReplay);
    }

    @Override
    @JsonProperty("authenticity")
    public String getAuthenticity() {
        return (String) data.get("authenticity");
    }

    @Override
    public void setAuthenticity(String authenticity) {
        putOrRemove("authenticity", authenticity);
    }

    @Override
    @JsonProperty("encryption")
    public String getEncryption() {
        return (String) data.get("encryption");
    }

    @Override
    public void setEncryption(String encryption) {
        putOrRemove("encryption", encryption);
    }

    @Override
    @SuppressWarnings("unchecked")
    @JsonProperty("custom")
    public Map<String, Object> getCustom() {
        Object custom = data.get("custom");
        if (custom instanceof Map) {
            return (Map<String, Object>) custom;
        }
        return null;
    }

    @Override
    public void setCustom(Map<String, Object> custom) {
        putOrRemove("custom", custom);
    }

    @Override
    @JsonProperty("timestamp")
    public Instant getTimestamp() {
        Object value = data.get("timestamp");
        if (value instanceof Instant) {
            return (Instant) value;
        }
        if (value instanceof String) {
            return Instant.parse((String) value);
        }
        return null;
    }

    @Override
    public void setTimestamp(Instant timestamp) {
        putOrRemove("timestamp", timestamp);
    }

    @Override
    @JsonProperty("source")
    public AppIdentifier getSource() {
        Object source = data.get("source");
        if (source instanceof AppIdentifier) {
            return (AppIdentifier) source;
        }
        if (source instanceof Map) {
            return AppIdentifier.fromMap((Map<String, Object>) source);
        }
        return null;
    }

    @Override
    public void setSource(AppIdentifier source) {
        putOrRemove("source", source);
    }

    public static ContextMetadata fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        ContextMetadata metadata = new ContextMetadata();
        metadata.mergeFrom(map);
        return metadata;
    }

    public void mergeFrom(Map<String, Object> map) {
        if (map == null) {
            return;
        }
        Object source = map.get("source");
        if (source != null) {
            setSource(source instanceof AppIdentifier ? (AppIdentifier) source : AppIdentifier.fromMap(castMap(source)));
        }
        Object timestamp = map.get("timestamp");
        if (timestamp instanceof Instant) {
            setTimestamp((Instant) timestamp);
        } else if (timestamp instanceof String) {
            setTimestamp(Instant.parse((String) timestamp));
        }
        setTraceId((String) map.get("traceId"));
        Object signature = map.get("signature");
        if (signature instanceof DetachedSignature) {
            setSignature((DetachedSignature) signature);
        } else if (signature instanceof Map) {
            setSignature(mapToDetachedSignature(castMap(signature)));
        }
        Object antiReplay = map.get("antiReplay");
        if (antiReplay instanceof AntiReplayClaims) {
            setAntiReplay((AntiReplayClaims) antiReplay);
        } else if (antiReplay instanceof Map) {
            setAntiReplay(mapToAntiReplay(castMap(antiReplay)));
        }
        Object custom = map.get("custom");
        if (custom instanceof Map) {
            setCustom(castMap(custom));
        }
        setAuthenticity((String) map.get("authenticity"));
        setEncryption((String) map.get("encryption"));
    }

    private void putOrRemove(String key, Object value) {
        if (value == null) {
            data.remove(key);
        } else {
            data.put(key, value);
        }
    }

    private <T> T getTyped(String key, Class<T> type) {
        Object value = data.get(key);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        if (value instanceof Map) {
            if (type == DetachedSignature.class) {
                return type.cast(mapToDetachedSignature(castMap(value)));
            }
            if (type == AntiReplayClaims.class) {
                return type.cast(mapToAntiReplay(castMap(value)));
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static DetachedSignature mapToDetachedSignature(Map<String, Object> map) {
        DetachedSignature sig = new DetachedSignature();
        sig.setProtectedHeader((String) map.get("protected"));
        sig.setSignature((String) map.get("signature"));
        return sig;
    }

    private static AntiReplayClaims mapToAntiReplay(Map<String, Object> map) {
        Object iat = map.get("iat");
        Object exp = map.get("exp");
        long iatVal = iat instanceof Number ? ((Number) iat).longValue() : Long.parseLong(String.valueOf(iat));
        long expVal = exp instanceof Number ? ((Number) exp).longValue() : Long.parseLong(String.valueOf(exp));
        return new AntiReplayClaims(iatVal, expVal, (String) map.get("jti"));
    }
}
