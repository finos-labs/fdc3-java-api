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

package org.finos.fdc3.proxy.schema;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;

import org.finos.fdc3.api.metadata.AntiReplayClaims;
import org.finos.fdc3.api.metadata.AppProvidableContextMetadata;
import org.finos.fdc3.api.metadata.ContextMetadata;
import org.finos.fdc3.api.metadata.DesktopAgentProvidableContextMetadata;
import org.finos.fdc3.api.metadata.DetachedSignature;
import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.proxy.util.ContextMetadataMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the hand-written metadata types in {@code fdc3-standard} against the DACP schemas.
 * <p>
 * {@code ContextRoundTripTest} does this for context types, which are generated from their
 * schemas and so cannot drift. The metadata types are hand-written, so nothing previously
 * stopped a field being added to (or dropped from) the Java API without a matching schema
 * change. These tests close that gap in both directions:
 * <ul>
 *   <li>the property names the Java interfaces expose must equal the schema's properties;</li>
 *   <li>everything a caller can set on outbound metadata must survive
 *       {@link ContextMetadataMapper#toWire} and validate against the schema.</li>
 * </ul>
 */
class MetadataSchemaConformanceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static JsonNode apiSchema;

    @BeforeAll
    static void loadApiSchema() throws IOException {
        Path apiDir = LoadSchemas.apiSchemaDirectory();
        Assumptions.assumeTrue(apiDir != null, "API schemas not available; build fdc3-schema first");
        Path apiSchemaFile = apiDir.resolve("api.schema.json");
        Assumptions.assumeTrue(Files.isRegularFile(apiSchemaFile), "api.schema.json not found in " + apiDir);
        apiSchema = MAPPER.readTree(apiSchemaFile.toFile());
    }

    @Test
    @DisplayName("AppProvidableContextMetadata exposes exactly the properties its schema allows")
    void appProvidableMetadataMatchesSchema() {
        assertPropertiesMatch("AppProvidableContextMetadata", javaProperties(AppProvidableContextMetadata.class));
    }

    @Test
    @DisplayName("ContextMetadata exposes exactly the properties its schema allows")
    void contextMetadataMatchesSchema() {
        Set<String> javaProperties = new LinkedHashSet<>(javaProperties(AppProvidableContextMetadata.class));
        javaProperties.addAll(javaProperties(DesktopAgentProvidableContextMetadata.class));
        assertPropertiesMatch("ContextMetadata", javaProperties);
    }

    @Test
    @DisplayName("Everything settable on outbound metadata reaches the wire and validates")
    void outboundMetadataSurvivesToWireAndValidates() throws IOException {
        ContextMetadata metadata = ContextMetadata.appProvidable();

        // Populate every field reachable through the outbound-facing interface.
        AppProvidableContextMetadata outbound = metadata;
        outbound.setTraceId("trace-1");
        DetachedSignature signature = new DetachedSignature();
        signature.setProtectedHeader("protected-header");
        signature.setSignature("signature-value");
        outbound.setSignature(signature);
        outbound.setAntiReplay(new AntiReplayClaims(1_700_000_000L, 1_700_003_600L, "nonce-1"));
        Map<String, Object> custom = new LinkedHashMap<>();
        custom.put("vendorField", "vendor-value");
        outbound.setCustom(custom);

        Map<String, Object> wire = ContextMetadataMapper.toWire(outbound);

        // Nothing a caller can set may be silently dropped on the way to the wire.
        assertEquals(javaProperties(AppProvidableContextMetadata.class), new LinkedHashSet<>(wire.keySet()),
                "Fields settable via AppProvidableContextMetadata but missing from the wire payload are "
                        + "silently dropped; either emit them in ContextMetadataMapper.toWire or remove "
                        + "them from the outbound interface");

        JsonSchema schema = LoadSchemas.definitionValidator("AppProvidableContextMetadata");
        Set<ValidationMessage> errors = schema.validate(MAPPER.valueToTree(wire));
        assertTrue(errors.isEmpty(), () -> "Outbound metadata failed schema validation: "
                + errors.stream().map(ValidationMessage::getMessage).collect(Collectors.joining("; ")));
    }

    @Test
    @DisplayName("Inbound metadata round-trips back to an equivalent wire payload")
    void inboundMetadataRoundTrips() throws IOException {
        Map<String, Object> signature = new LinkedHashMap<>();
        signature.put("protected", "protected-header");
        signature.put("signature", "signature-value");
        Map<String, Object> antiReplay = new LinkedHashMap<>();
        antiReplay.put("iat", 1_700_000_000L);
        antiReplay.put("exp", 1_700_003_600L);
        antiReplay.put("jti", "nonce-1");
        Map<String, Object> custom = new LinkedHashMap<>();
        custom.put("vendorField", "vendor-value");

        Map<String, Object> incoming = new LinkedHashMap<>();
        incoming.put("traceId", "trace-1");
        incoming.put("signature", signature);
        incoming.put("antiReplay", antiReplay);
        incoming.put("custom", custom);

        ContextMetadata metadata = ContextMetadataMapper.fromWire(incoming, Instant.now());
        metadata.setSource(new AppIdentifier("app-1", "instance-1"));

        assertEquals("trace-1", metadata.getTraceId());
        assertEquals("protected-header", metadata.getSignature().getProtectedHeader());
        assertEquals("nonce-1", metadata.getAntiReplay().getJti());

        // Round-trip back out and confirm it still satisfies the outbound schema.
        Map<String, Object> wire = ContextMetadataMapper.toWire(metadata);
        assertEquals(incoming.keySet(), wire.keySet(), "Round-tripped metadata lost or gained fields");

        JsonSchema schema = LoadSchemas.definitionValidator("AppProvidableContextMetadata");
        Set<ValidationMessage> errors = schema.validate(MAPPER.valueToTree(wire));
        assertTrue(errors.isEmpty(), () -> "Round-tripped metadata failed schema validation: "
                + errors.stream().map(ValidationMessage::getMessage).collect(Collectors.joining("; ")));
    }

    private void assertPropertiesMatch(String definitionName, Collection<String> javaProperties) {
        Set<String> schemaProperties = schemaProperties(definitionName);
        Set<String> inJavaOnly = new TreeSet<>(javaProperties);
        inJavaOnly.removeAll(schemaProperties);
        Set<String> inSchemaOnly = new TreeSet<>(schemaProperties);
        inSchemaOnly.removeAll(javaProperties);

        assertTrue(inJavaOnly.isEmpty() && inSchemaOnly.isEmpty(),
                () -> "Java metadata types have drifted from api.schema.json#/definitions/" + definitionName
                        + "\n  exposed by Java but absent from the schema: " + inJavaOnly
                        + "\n  declared in the schema but missing from Java: " + inSchemaOnly);
    }

    private Set<String> schemaProperties(String definitionName) {
        JsonNode definition = apiSchema.path("definitions").path(definitionName);
        assertTrue(definition.isObject(), "api.schema.json has no definition named " + definitionName);
        Set<String> properties = new LinkedHashSet<>();
        definition.path("properties").fieldNames().forEachRemaining(properties::add);
        return properties;
    }

    /**
     * The JSON property names an interface exposes, derived from its getters (including
     * inherited ones), honouring {@link JsonProperty} overrides.
     */
    private static Set<String> javaProperties(Class<?> type) {
        Set<String> properties = new LinkedHashSet<>();
        for (Method method : type.getMethods()) {
            if (method.getParameterCount() != 0 || method.getDeclaringClass() == Object.class) {
                continue;
            }
            String name = method.getName();
            String property;
            if (name.startsWith("get") && name.length() > 3) {
                property = name.substring(3);
            } else if (name.startsWith("is") && name.length() > 2) {
                property = name.substring(2);
            } else {
                continue;
            }
            JsonProperty annotation = method.getAnnotation(JsonProperty.class);
            if (annotation != null && !annotation.value().isEmpty()) {
                properties.add(annotation.value());
            } else {
                properties.add(Character.toLowerCase(property.charAt(0)) + property.substring(1));
            }
        }
        return properties;
    }
}
