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

package org.finos.fdc3.api.metadata;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DetachedSignature {

    private String protectedHeader;
    private String signature;

    public DetachedSignature() {
    }

    public DetachedSignature(String protectedHeader, String signature) {
        this.protectedHeader = protectedHeader;
        this.signature = signature;
    }

    @JsonProperty("protected")
    public String getProtectedHeader() {
        return protectedHeader;
    }

    public void setProtectedHeader(String protectedHeader) {
        this.protectedHeader = protectedHeader;
    }

    @JsonProperty("signature")
    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        DetachedSignature that = (DetachedSignature) other;
        return Objects.equals(protectedHeader, that.protectedHeader)
                && Objects.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protectedHeader, signature);
    }

    /** Omits the signature value itself, which is long and not useful in a log line. */
    @Override
    public String toString() {
        return "DetachedSignature{protected=" + protectedHeader
                + ", signature=" + (signature == null ? "null" : "present") + "}";
    }
}
