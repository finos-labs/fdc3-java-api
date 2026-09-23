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

import java.util.LinkedHashMap;
import java.util.Map;

import org.finos.cucumbertestingsteps.support.MatchingUtils;
import org.finos.cucumbertestingsteps.world.PropsWorld;
import org.finos.fdc3.api.metadata.ContextMetadata;
import org.finos.fdc3.api.metadata.DetachedSignature;
import org.finos.fdc3.api.types.AppIdentifier;
import org.finos.fdc3.api.types.ContextWithMetadata;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Fdc3SignatureMatcherTest {

    @BeforeAll
    static void registerMatchers() {
        Fdc3SchemaMatchers.registerFdc3SchemaMatchers();
    }

    @Test
    void matchesSignatureProtectedOnContextMetadata() {
        ContextMetadata metadata = ContextMetadata.appProvidable();
        metadata.setSource(new AppIdentifier("cucumber-app", "cucumber-instance"));
        DetachedSignature signature = new DetachedSignature();
        signature.setProtectedHeader("test-sig (protected part)");
        signature.setSignature("test-sig (signature part)");
        metadata.setSignature(signature);

        Map<String, String> row = new LinkedHashMap<>();
        row.put("signature.signature", "test-sig (signature part)");
        row.put("signature.protected", "test-sig (protected part)");

        PropsWorld world = new PropsWorld();
        assertTrue(MatchingUtils.doesRowMatch(world, row, metadata));
    }

    @Test
    void matchesMetadataSignatureProtectedOnContextWithMetadata() {
        ContextMetadata metadata = ContextMetadata.appProvidable();
        DetachedSignature signature = new DetachedSignature();
        signature.setProtectedHeader("test-signature (protected part)");
        signature.setSignature("test-signature (signature part)");
        metadata.setSignature(signature);

        ContextWithMetadata result = new ContextWithMetadata(null, metadata);

        Map<String, String> row = new LinkedHashMap<>();
        row.put("metadata.signature.signature", "test-signature (signature part)");
        row.put("metadata.signature.protected", "test-signature (protected part)");

        PropsWorld world = new PropsWorld();
        assertTrue(MatchingUtils.doesRowMatch(world, row, result));
    }
}
