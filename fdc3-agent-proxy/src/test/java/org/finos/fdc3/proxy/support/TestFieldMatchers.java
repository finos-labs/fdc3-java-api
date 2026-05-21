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
package org.finos.fdc3.proxy.support;

import static io.github.robmoffat.support.MatchingUtils.handleResolve;
import static io.github.robmoffat.support.MatchingUtils.pathForFieldSuffix;
import static io.github.robmoffat.support.MatchingUtils.registerFieldMatcher;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathNotFoundException;
import org.finos.fdc3.api.metadata.AntiReplayClaims;
import org.finos.fdc3.api.metadata.ContextMetadata;
import org.finos.fdc3.api.metadata.DetachedSignature;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.robmoffat.support.RowFieldMatcher;
import io.github.robmoffat.world.PropsWorld;

/**
 * Field matchers for FDC3-specific assertion columns in Cucumber tables.
 */
public final class TestFieldMatchers {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SIGNATURE_SUFFIX = ".signature";
    private static final String IAT_SUFFIX = ".iat";

    static {
        registerFieldMatcher(signatureFieldMatcher());
        registerFieldMatcher(antiReplayIatMatcher());
    }

    private TestFieldMatchers() {
    }

    private static RowFieldMatcher signatureFieldMatcher() {
        return new RowFieldMatcher() {
            @Override
            public boolean matchesField(String field) {
                return "signature".equals(field) || field.endsWith(SIGNATURE_SUFFIX);
            }

            @Override
            public boolean matchField(PropsWorld world, String field, String expected, Object rowData) {
                String path = "signature".equals(field) ? "signature" : pathForFieldSuffix(field, SIGNATURE_SUFFIX);
                if (path == null) {
                    return false;
                }
                Object found = valueAtPath(rowData, path);
                String actual = signatureValue(found);
                Object resolved = handleResolve(expected, world);
                if (!Objects.equals(asString(actual), asString(resolved))) {
                    world.log(String.format("Signature match failed on %s: '%s' vs '%s'", field, actual, resolved));
                    return false;
                }
                return true;
            }
        };
    }

    private static RowFieldMatcher antiReplayIatMatcher() {
        return new RowFieldMatcher() {
            @Override
            public boolean matchesField(String field) {
                return field.endsWith(IAT_SUFFIX);
            }

            @Override
            public boolean matchField(PropsWorld world, String field, String expected, Object rowData) {
                String path = pathForFieldSuffix(field, IAT_SUFFIX);
                if (path == null) {
                    return false;
                }
                Object found = valueAtPath(rowData, path);
                Object resolved = handleResolve(expected, world);
                if (!numericEquals(found, resolved)) {
                    world.log(String.format("Numeric match failed on %s: '%s' vs '%s'", field, found, resolved));
                    return false;
                }
                return true;
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> metadataToAssertionMap(ContextMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        return MAPPER.convertValue(metadata, Map.class);
    }

    private static String signatureValue(Object found) {
        if (found == null) {
            return null;
        }
        if (found instanceof DetachedSignature) {
            return ((DetachedSignature) found).getSignature();
        }
        if (found instanceof Map) {
            Object inner = ((Map<?, ?>) found).get("signature");
            return inner == null ? null : String.valueOf(inner);
        }
        return String.valueOf(found);
    }

    private static boolean numericEquals(Object found, Object expected) {
        if (found == null && expected == null) {
            return true;
        }
        if (found == null || expected == null) {
            return false;
        }
        if (found instanceof Number && expected instanceof Number) {
            return ((Number) found).longValue() == ((Number) expected).longValue();
        }
        String foundStr = String.valueOf(found);
        String expectedStr = String.valueOf(expected);
        if (Pattern.matches("-?\\d+", foundStr) && Pattern.matches("-?\\d+(\\.0)?", expectedStr)) {
            return Long.parseLong(foundStr) == (long) Double.parseDouble(expectedStr);
        }
        return Objects.equals(foundStr, expectedStr);
    }

    private static Object valueAtPath(Object data, String path) {
        if (path.isEmpty()) {
            return data;
        }
        try {
            JXPathContext context = JXPathContext.newContext(data);
            context.setLenient(true);
            String xpathName = "/" + path.replace('.', '/');
            return context.getValue(xpathName);
        } catch (JXPathNotFoundException e) {
            return null;
        }
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
