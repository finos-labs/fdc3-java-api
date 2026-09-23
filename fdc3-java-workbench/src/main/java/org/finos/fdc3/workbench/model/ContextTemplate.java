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

package org.finos.fdc3.workbench.model;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A named context template used by the workbench for broadcast / raiseIntent.
 */
public class ContextTemplate {

    private final String uuid;
    private String id;
    private JsonNode template;

    public ContextTemplate(String id, JsonNode template) {
        this(UUID.randomUUID().toString(), id, template);
    }

    public ContextTemplate(String uuid, String id, JsonNode template) {
        this.uuid = uuid;
        this.id = id;
        this.template = template;
    }

    public String getUuid() {
        return uuid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public JsonNode getTemplate() {
        return template;
    }

    public void setTemplate(JsonNode template) {
        this.template = template;
    }

    public String contextType() {
        if (template != null && template.has("type") && template.get("type").isTextual()) {
            return template.get("type").asText();
        }
        return "";
    }

    @Override
    public String toString() {
        return id;
    }

    public Map<String, Object> asMap(com.fasterxml.jackson.databind.ObjectMapper mapper) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = mapper.convertValue(template, Map.class);
        return map;
    }
}
