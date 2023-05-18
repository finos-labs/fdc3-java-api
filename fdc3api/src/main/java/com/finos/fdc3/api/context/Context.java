/**
 * Copyright 2023 Wellington Management Company LLP
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

package com.finos.fdc3.api.context;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.finos.fdc3.api.utils.StringUtilities;

import java.util.Map;

@JsonPropertyOrder({
        "type",
        "name",
        "id"
})
public class Context
{
    @JsonProperty("type")
    private String type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("id")
    private Map<Object, Object> id;

    public Context() {}
    public Context(String type) {
        this.type = type;
        this.name = null;
        this.id = null;
    }

    public Context(String type, String name) {
        this.type = type;
        this.name = name;
        this.id = null;
    }

    public Context(String type, Map<Object, Object> id) {
        this.type = type;
        this.name = null;
        this.setId(id);
    }

    public Context(String type, String name, Map<Object, Object> id) {
        this.type = type;
        this.name = name;
        this.setId(id);
    }

    public String getType()
    {
        return type;
    }

    public Map<Object, Object>  getId()
    {
        return id;
    }

    public void setId(Map<Object, Object>  id)
    {
        /*
        Map<String, Boolean> keyMap = ContextContants.CONTEXT_ID_KEYS_MAPPING.getOrDefault(type, null);
        if(!ContextHelper.hasValidKeys(id, keyMap)) {
            throw new IllegalArgumentException("Invalid ID(s) present!");
        }
        if(!ContextHelper.hasMandatoryKeys(id, keyMap)) {
            throw new IllegalArgumentException("Mandatory ID(s) missing!");
        }
        */

        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return StringUtilities.valueAsString(this);
    }

}
