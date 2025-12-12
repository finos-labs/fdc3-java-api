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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ContextHelper {
    public static Set<Object> getMandatoryIdKeys(Map<String, Boolean> validIdKeys) {
        Set<Object> mandatoryIdKeys = new HashSet<>();
        if(validIdKeys == null) {
            return mandatoryIdKeys;
        }
        for(Map.Entry<String, Boolean> entry : validIdKeys.entrySet()) {
            if(entry.getValue()) {
                mandatoryIdKeys.add(entry.getKey());
            }
         }
        return mandatoryIdKeys;
    }

    public static boolean hasMandatoryKeys(Map<Object, Object> idMap, Map<String, Boolean> validIdKeys) {
        Set<Object> mandatoryIdKeys = ContextHelper.getMandatoryIdKeys(validIdKeys);
        if(mandatoryIdKeys.isEmpty()) {
            return true;
        } else if (idMap == null) {
            return false;
        }
        for(Object key : mandatoryIdKeys) {
            if(!idMap.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasValidKeys(Map<Object, Object> idMap, Map<String, Boolean> validIdKeys) {
        if (idMap == null) {
            return true;
        }
        if (validIdKeys == null) {
            return false;
        }
        if (validIdKeys.isEmpty()) {
            return true;
        }
        for(Object key: idMap.keySet()) {
            if(!validIdKeys.containsKey(key)) {
                return false;
            }
        }
        return true;
    }
}
