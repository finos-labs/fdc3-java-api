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

package org.finos.fdc3.workbench.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

import org.finos.fdc3.workbench.model.ContextTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Context template library with defaults from classpath and Preferences persistence.
 */
public class ContextTemplateStore {

    private static final String PREF_KEY = "contextTemplates";

    private final ObjectMapper mapper;
    private final SystemLogService log;
    private final Preferences prefs = Preferences.userNodeForPackage(ContextTemplateStore.class);
    private final ObservableList<ContextTemplate> templates = FXCollections.observableArrayList();

    public ContextTemplateStore(ObjectMapper mapper, SystemLogService log) {
        this.mapper = mapper;
        this.log = log;
        load();
    }

    public ObservableList<ContextTemplate> getTemplates() {
        return templates;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public void load() {
        templates.clear();
        String saved = prefs.get(PREF_KEY, null);
        if (saved != null && !saved.isBlank()) {
            try {
                templates.addAll(parseList(mapper.readTree(saved)));
                log.info("contexts", "Loaded " + templates.size() + " context templates from preferences");
                return;
            } catch (IOException e) {
                log.error("contexts", "Failed to load saved templates - " + e.getMessage());
            }
        }
        resetToDefaults();
    }

    public void resetToDefaults() {
        templates.clear();
        try (InputStream in = ContextTemplateStore.class.getResourceAsStream("/contexts-default.json")) {
            if (in == null) {
                log.error("contexts", "contexts-default.json not found on classpath");
                return;
            }
            templates.addAll(parseList(mapper.readTree(in)));
            persist();
            log.info("contexts", "Reset to " + templates.size() + " default context templates");
        } catch (IOException e) {
            log.error("contexts", "Failed to load default templates - " + e.getMessage());
        }
    }

    public void add(ContextTemplate template) {
        templates.add(template);
        persist();
    }

    public void update(ContextTemplate template) {
        persist();
    }

    public void remove(ContextTemplate template) {
        templates.remove(template);
        persist();
    }

    public void persist() {
        try {
            ArrayNode array = mapper.createArrayNode();
            for (ContextTemplate t : templates) {
                ObjectNode node = mapper.createObjectNode();
                node.put("uuid", t.getUuid());
                node.put("id", t.getId());
                node.set("template", t.getTemplate());
                array.add(node);
            }
            prefs.put(PREF_KEY, mapper.writeValueAsString(array));
        } catch (IOException | IllegalArgumentException e) {
            // macOS Java Preferences reject values over ~8KB ("Value too long")
            log.error("contexts", "Failed to persist templates - " + e.getMessage());
        }
    }

    private List<ContextTemplate> parseList(JsonNode root) {
        List<ContextTemplate> list = new ArrayList<>();
        if (root == null || !root.isArray()) {
            return list;
        }
        for (Iterator<JsonNode> it = root.elements(); it.hasNext();) {
            JsonNode node = it.next();
            String id = node.path("id").asText("unnamed");
            JsonNode template = node.get("template");
            String uuid = node.has("uuid") ? node.get("uuid").asText() : null;
            if (uuid != null) {
                list.add(new ContextTemplate(uuid, id, template));
            } else {
                list.add(new ContextTemplate(id, template));
            }
        }
        return list;
    }
}
