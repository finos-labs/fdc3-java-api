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

package com.finos.fdc3.api.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class JacksonUtilities
{
private static final ObjectMapper jacksonObjectMapper = new ObjectMapper();
private JacksonUtilities() {
}

public static String serializeToString(Object obj) throws Exception {
    if (obj == null) {
        throw new Exception("null obj specified");
    } else {
        try {
            return getObjectMapper().writeValueAsString(obj);
        } catch (Throwable var2) {
            throw new Exception(String.format("error serializaing obj, details: %s - %s", var2.getMessage(), var2.getCause()), var2);
        }
    }
}

public static String serializeToPrettyString(Object obj) throws Exception {
    if (obj == null) {
        throw new Exception("null obj specified");
    } else {
        try {
            return getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Throwable var2) {
            throw new Exception(String.format("error serializaing (pretty) obj, details: %s - %s", var2.getMessage(), var2.getCause()), var2);
        }
    }
}

public static <T> T deserializeFromString(String str, Class<T> cl) throws Exception {
    if (str == null) {
        throw new Exception("null str specified");
    } else if (cl == null) {
        throw new Exception("null cl specified");
    } else {
        try {
            return getObjectMapper().readValue(str, cl);
        } catch (Throwable var3) {
            throw new Exception(String.format("error deserializing str '%s' for class: %s, details: %s - %s", str, cl.getName(), var3.getMessage(), var3.getCause()), var3);
        }
    }
}

public static <T> List<T> deserializeListFromString(String message, Class<T> cl) throws Exception {
    if (message == null) {
        throw new Exception("null message specified");
    } else if (cl == null) {
        throw new Exception("null cl specified");
    } else {
        try {
            return (List)getObjectMapper().readValue(message, getObjectMapper().getTypeFactory().constructCollectionType(List.class, cl));
        } catch (Throwable var3) {
            throw new Exception(String.format("error deserializing list for class: %s from message: %s, details: %s - %s", cl.getName(), message, var3.getMessage(), var3.getCause()), var3);
        }
    }
}

public static ObjectMapper getObjectMapper() {
    return jacksonObjectMapper;
}


}
