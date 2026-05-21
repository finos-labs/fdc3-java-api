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

/**
 * Security-related metadata fields shared across context and intent metadata types.
 */
public interface SecurityMetadata {

    DetachedSignature getSignature();

    void setSignature(DetachedSignature signature);

    AntiReplayClaims getAntiReplay();

    void setAntiReplay(AntiReplayClaims antiReplay);

    /**
     * Result of signature verification by the receiving app's security layer.
     */
    String getAuthenticity();

    void setAuthenticity(String authenticity);

    /**
     * Result of decryption by the receiving app's security layer.
     */
    String getEncryption();

    void setEncryption(String encryption);
}
