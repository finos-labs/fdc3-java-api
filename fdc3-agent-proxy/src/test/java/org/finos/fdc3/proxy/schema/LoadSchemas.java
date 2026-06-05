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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SpecVersion;

import io.github.robmoffat.world.PropsWorld;

/**
 * Loads FDC3 JSON schemas from local files into the test world.
 * Java equivalent of {@code @finos/fdc3-schema/test/loadSchemas.ts} (Ajv {@code addSchema} pool).
 */
public final class LoadSchemas {

    private static final String SCHEMA_BASE = "https://fdc3.finos.org/schemas/next/api/";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String[] API_SCHEMA_LOAD_ORDER = {
            "api.schema.json",
            "common.schema.json",
            "appRequest.schema.json",
            "agentRequest.schema.json",
            "agentResponse.schema.json",
            "appResponse.schema.json",
    };

    private static volatile JsonSchemaFactory schemaFactory;
    /** Compiled validators keyed by short schema id (e.g. {@code broadcastRequest}). */
    private static volatile Map<String, JsonSchema> schemaValidators;

    private LoadSchemas() {
    }

    public static void loadSchemasIntoWorld(PropsWorld world) throws IOException {
        if (world.get("schemas") != null) {
            return;
        }
        world.set("schemas", loadSchemaValidators());
        world.set("jsonSchemaFactory", schemaFactory);
    }

    public static Map<String, JsonSchema> loadSchemaValidators() throws IOException {
        Map<String, JsonSchema> cached = schemaValidators;
        if (cached != null) {
            return cached;
        }
        synchronized (LoadSchemas.class) {
            if (schemaValidators != null) {
                return schemaValidators;
            }
            Path apiDir = resolveApiSchemaDirectory();
            if (apiDir == null || !Files.isDirectory(apiDir)) {
                throw new IllegalStateException(
                        "Schema directory not found. Build fdc3-schema first (target/schema-work/api) or use FDC3 monorepo schemas at "
                                + "../../../FDC3/packages/fdc3-schema/schemas/api");
            }

            Map<String, String> schemasByIri = loadSchemaDocuments(apiDir, resolveContextSchemaFile());

            JsonSchemaFactory factory = JsonSchemaFactory.builder(
                            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
                    .schemaLoaders(loaders -> loaders.schemas(schemasByIri))
                    .build();

            Map<String, JsonSchema> validators = new HashMap<>();
            for (Path file : orderSchemaFiles(apiDir)) {
                JsonNode node = MAPPER.readTree(file.toFile());
                String shortId = schemaIdFromFile(node, file.getFileName().toString());
                if (node.has("$id")) {
                    validators.put(shortId, factory.getSchema(SchemaLocation.of(node.get("$id").asText())));
                }
            }

            schemaFactory = factory;
            schemaValidators = Collections.unmodifiableMap(validators);
            return schemaValidators;
        }
    }

    public static String schemaUri(String schemaId) {
        return SCHEMA_BASE + schemaId + ".schema.json";
    }

    private static Map<String, String> loadSchemaDocuments(Path apiDir, Path contextSchemaFile) throws IOException {
        Map<String, String> schemasByIri = new HashMap<>();

        for (Path file : orderSchemaFiles(apiDir)) {
            String content = Files.readString(file);
            JsonNode node = MAPPER.readTree(content);
            String filename = file.getFileName().toString();
            schemasByIri.put(filename, content);
            if (node.has("$id")) {
                schemasByIri.put(node.get("$id").asText(), content);
            }
            schemasByIri.put(file.toUri().toString(), content);
        }

        if (contextSchemaFile != null && Files.exists(contextSchemaFile)) {
            String content = Files.readString(contextSchemaFile);
            JsonNode node = MAPPER.readTree(content);
            schemasByIri.put("../context/context.schema.json", content);
            schemasByIri.put(contextSchemaFile.toUri().toString(), content);
            if (node.has("$id")) {
                schemasByIri.put(node.get("$id").asText(), content);
            }
        }

        return schemasByIri;
    }

    private static List<Path> orderSchemaFiles(Path apiDir) throws IOException {
        List<Path> ordered = new java.util.ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (String name : API_SCHEMA_LOAD_ORDER) {
            Path p = apiDir.resolve(name);
            if (Files.exists(p)) {
                ordered.add(p);
                seen.add(name);
            }
        }

        try (Stream<Path> files = Files.list(apiDir)) {
            ordered.addAll(files.filter(p -> p.toString().endsWith(".json"))
                    .filter(p -> !seen.contains(p.getFileName().toString()))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList()));
        }
        return ordered;
    }

    private static Path resolveApiSchemaDirectory() {
        Path[] candidates = {
                Paths.get("../fdc3-schema/target/schema-work/api"),
                Paths.get("../../../FDC3/packages/fdc3-schema/schemas/api"),
                Paths.get("../fdc3-schema/target/npm-work/node_modules/@finos/fdc3-schema/dist/schemas/api"),
                Paths.get("../fdc3-schema/schemas/api"),
        };
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return candidate.normalize();
            }
        }
        return null;
    }

    private static Path resolveContextSchemaFile() {
        Path[] candidates = {
                Paths.get("../fdc3-schema/target/schema-work/context/context.schema.json"),
                Paths.get("../../../FDC3/packages/fdc3-context/schemas/context/context.schema.json"),
                Paths.get("../fdc3-context/target/npm-work/node_modules/@finos/fdc3-context/dist/schemas/context/context.schema.json"),
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate.normalize();
            }
        }
        return null;
    }

    private static String schemaIdFromFile(JsonNode schemaJson, String filename) {
        if (schemaJson.has("$id")) {
            String id = schemaJson.get("$id").asText();
            if (id.startsWith(SCHEMA_BASE)) {
                return id.substring(SCHEMA_BASE.length()).replace(".schema.json", "");
            }
            return id;
        }
        return filename.replace(".schema.json", "").replace(".json", "");
    }
}
